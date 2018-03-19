package com.bignerdranch.android.harmonize_audio_player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.media.session.MediaSession;
import android.media.session.MediaController;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnInfoListener,MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private final IBinder iBinder  = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private int resumePosition;
    private AudioManager audioManager;

    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    public static String repeat = "stopRepeat";

    public String TOWARDS_SOLO = "SEND DATA";

    public static Boolean shuffle = false;


    public static Boolean isPlaying = false;

    //List of available Audio files
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio; //an object of the currently playing audio
    public static final String ACTION_PLAY = "com.valdioveliu.valdio.audioplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.valdioveliu.valdio.audioplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.valdioveliu.valdio.audioplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.valdioveliu.valdio.audioplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.valdioveliu.valdio.audioplayer.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaSession meSession;
    private MediaController.TransportControls transControls;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    //Notifier for resume position
    private static String resumeChecker = null;
    private static int resumeMusic;

    public static String audibility = "notSilent";

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        meSession = new MediaSession(getApplicationContext(),"AudioPlayer");

        //Get MediaSessions transport controls
        transControls = meSession.getController().getTransportControls();

        //set MediaSession -> ready to receive media commands
        meSession.setActive(true);

        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        meSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);


        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        meSession.setCallback(new MediaSession.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING,true);

            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED,false);

            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING,true);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING,true);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();

            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transControls.stop();
        }
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void buildNotification(PlaybackStatus playbackStatus,boolean persistent) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.image); //replace with your own image

        // Create a new Notification
       Notification notificationBuilder = new Notification.Builder(MediaPlayerService.this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new Notification.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(meSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information

                .setContentText(activeAudio.getArtist())
                .setContentTitle(activeAudio.getTitle())
                .setContentInfo(activeAudio.getAlbum())
                .setOngoing(persistent)
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID,notificationBuilder);
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.image); //replace with medias albumArt
        // Update the current metadata
        meSession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadata.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    private Integer shuffler(Integer currentPosition,Integer audioSize){
        Integer returnValue = 0;
        Random r = new Random();
        int shuffleNumber = r.nextInt(audioList.size() - 0) + 0;

        returnValue = currentPosition + shuffleNumber;
        if(returnValue >= audioSize){
            returnValue = returnValue %(audioSize - 1);
            return returnValue;
        }
        else{
            return returnValue;
        }

    }

    private void skipToNext() {

             resumeChecker = null;


            if (audioIndex == audioList.size() - 1) {
                //if last in playlist
                audioIndex = 0;
                if(shuffle) {
                    audioIndex = shuffler(audioIndex,audioList.size());
                }
                activeAudio = audioList.get(audioIndex);
            } else {
                //get next in playlist
                if(shuffle) {
                    audioIndex = shuffler(audioIndex,audioList.size());
                    activeAudio = audioList.get(audioIndex);
                }
                else {
                    activeAudio = audioList.get(++audioIndex);
                }
            }

            //Update stored index
            new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

            stopMedia();
            //reset mediaPlayer
            mediaPlayer.reset();
            initMediaPlayer();
        sendToSoloIntent("sendMusicName",-1);
    }

    private void skipToPrevious() {

        resumeChecker = null;

        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
        sendToSoloIntent("sendMusicName",-1);
    }


    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (requestAudioFocus()) {
                //Get the new media index form SharedPreferences
                audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
                if (audioIndex != -1 && audioIndex < audioList.size()) {
                    //index is in a valid range
                    activeAudio = audioList.get(audioIndex);
                } else {
                    stopSelf();
                }

                //A PLAY_NEW_AUDIO action received
                //reset mediaPlayer to play the new Audio
                stopMedia();
                mediaPlayer.reset();
                initMediaPlayer();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING, true);
            } else {

            }
        }
    };

    private void sendToSoloIntent(String message, Integer duration){
        Intent broadcastIntent = new Intent(TOWARDS_SOLO);
        Log.i("Hey",message);
        if(message.equals("changePlayPause")){
            broadcastIntent.putExtra("FromService",message);
            broadcastIntent.putExtra("changeIcon",message);

        }
        else if(message.equals("sendMusicName")){
            broadcastIntent.putExtra("FromService",message);
            broadcastIntent.putExtra("MusicName",audioList.get(audioIndex).getTitle());
            broadcastIntent.putExtra("musicArtist",audioList.get(audioIndex).getArtist());
            MainActivity.musicArtist = audioList.get(audioIndex).getArtist();
        }
        else if(message.equals("getCurrentPosition")){
            broadcastIntent.putExtra("FromService",message);
            broadcastIntent.putExtra(message,duration);
        }
        else if(message.equals("changingLayout")){
            broadcastIntent.putExtra("FromService","changingLayout");
            broadcastIntent.putExtra("resumingMusic",audioList.get(audioIndex).getTitle());
        }
        else{
            broadcastIntent.putExtra("FromService",message);
            broadcastIntent.putExtra("duration",duration);
        }
        sendBroadcast(broadcastIntent);
    }

    private BroadcastReceiver playControls = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String controlMessage = intent.getExtras().getString("control");
            Log.i("service",controlMessage);
            if(controlMessage.equals("Next")){
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING,true);
            }
            else if(controlMessage.equals("Prev")){
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING,true);
            }

            else if(controlMessage.equals("Play")){
                if(requestAudioFocus()){
                    resumeMedia();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING,true);
                }
                else {

                }

            }
            else if (controlMessage.equals("repeat")){
                repeat = "repeat";
                shuffle = false;
            }
            else if (controlMessage.equals("stopRepeat")){
                repeat = "stopRepeat";
                shuffle = false;
            }
            else if (controlMessage.equals("silenceMusic")){
                audibility = "silent";
                mediaPlayer.setVolume(0,0);
            }
            else if(controlMessage.equals("unSilenceMusic")){
                audibility = "notSilent";
                mediaPlayer.setVolume(1,1);
            }

            else if(controlMessage.equals("shuffle")){
                shuffle = true;
            }

            else if(controlMessage.equals("getCurrentPosition")){
                sendToSoloIntent("currentPositionSeek",mediaPlayer.getCurrentPosition());
            }
            else if(controlMessage.equals("seekChange")){
                mediaPlayer.seekTo(intent.getExtras().getInt("seekBarChange"));
                if(!mediaPlayer.isPlaying()){
                    resumePosition = intent.getExtras().getInt("seekBarChange");
                }
            }
            else if(controlMessage.equals("changingLayout")){
                    sendToSoloIntent("changingLayout",-1);
                sendToSoloIntent("nothingHere",mediaPlayer.getDuration());
            }
            else{
                pauseMedia();
                updateMetaData();
                buildNotification(PlaybackStatus.PAUSED,false);
            }

        }
    };

    private void register_ControlAudio() {


        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(SoloMusicActivity.CONTROL);
        registerReceiver(playControls, filter);
    }

    private void register_playNewAudio() {


            //Register playNewMedia receiver
            IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO
            );
            registerReceiver(playNewAudio, filter);
        }



    private void playMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
            resumeChecker = null;
            sendToSoloIntent("nothingHere",mediaPlayer.getDuration());
            isPlaying = true;
            if(SoloMusicActivity.playPause.equals("notPlaying")){
                sendToSoloIntent("changePlayPause",-1);
            }
            if(audibility.equals("silent")){
                mediaPlayer.setVolume(0,0);
            }
        }
    }
    private void stopMedia(){

        if(mediaPlayer == null) return;
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            isPlaying = false;
        }
        if(audibility.equals("silent")){
            mediaPlayer.setVolume(0,0);
        }

    }

    private void pauseMedia(){

        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            resumeChecker = "data";
            resumeMusic = audioIndex;
            isPlaying = false;
            if(SoloMusicActivity.playPause.equals("playing")){
                Log.i("life","bitch");
                sendToSoloIntent("changePlayPause",-1);
            }



        }
    }

    private void resumeMedia(){

         if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            isPlaying = true;
            if(SoloMusicActivity.playPause.equals("notPlaying")){
                sendToSoloIntent("changePlayPause",-1);
            }
            // Time Stamp;
            Log.i("hello","nice " + mediaPlayer.getCurrentPosition() );
             if(audibility.equals("silent")){
                 mediaPlayer.setVolume(0,0);
             }
        }
    }


    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try{
            mediaPlayer.setDataSource(activeAudio.getData());
        }
        catch(IOException e){
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();
    }

    public MediaPlayerService() {

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(shuffle) {
            skipToNext();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING, true);
        }

        else if(repeat.equals("repeat")){
            playMedia();
        }
        else if(repeat.equals("stopRepeat")){
            if(audioIndex == (audioList.size() - 1)){
                pauseMedia();
                removeNotification();

            }
            else{
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING, true);
            }
        }
        else{
            skipToNext();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING, true);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if(resumeChecker == null){

        }
        else{
            if(audioIndex == resumeMusic){
                mediaPlayer.seekTo(resumePosition);
            }
        }
        playMedia();

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.i("focusgot","focus got");
                buildNotification(PlaybackStatus.PAUSED,false);
                // Lost focus for an unbounded amount of time: stop playback and release media player

                pauseMedia();


                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
//            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                // Lost focus for a short time, but it's ok to keep playing
//                // at an attenuated level
//
//                if (mediaPlayer.isPlaying()) {
//                    mediaPlayer.setVolume(0.1f, 0.1f);
//
//                }
//                break;
        }
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }
    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING,true);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();
        register_ControlAudio();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(playControls);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();

            buildNotification(PlaybackStatus.PAUSED,true);


        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;

                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();

                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }
}


