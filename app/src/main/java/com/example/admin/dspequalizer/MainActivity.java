package com.example.admin.dspequalizer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gauravk.audiovisualizer.visualizer.BarVisualizer;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements DialogListener {
    public static long[] mag;
    private static int audioCurrentPosition;
    public static int currentVolume;
    private ArrayList<VerticalSeekBar> verticalSeekBars;
    private LinearLayout eqLinearLayout;
    private VisualizerView timeSpectrum;
    private MediaPlayer mMediaPlayer;
    private Visualizer mVisualizer;
    private Equalizer mEqualizer;
    private Button selectFileButton;
    private Button playButton;
    private Button stopButton;
    private SeekBar volume;
    private boolean spectrumChanged = false;
//    GraphView graph;
    private BarVisualizer bar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Initialize();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mMediaPlayer!=null){
            mMediaPlayer.release();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
        }
    }

    private void Initialize() {
        initID();
        setupEqualizerBands();
        volumeSetup();
        onClickButton();

    }

    private void onClickButton() {
        onClickSelectButton();
        onClickPlayButton();
        onClickStopButton();
    }

    private void initAudio(Uri mMediaPlayerUri) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if(mMediaPlayerUri == null){
            Toast.makeText(this,"Can't find your audio sound",Toast.LENGTH_SHORT).show();
        }
        else{
            mMediaPlayer = MediaPlayer.create(this, mMediaPlayerUri);
            setUpTimeSpectum();
            mVisualizer.setEnabled(true);
            mMediaPlayer
                    .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mVisualizer.setEnabled(false);
                        }
                    });
            mMediaPlayer.setVolume((currentVolume / 100.0f), (currentVolume / 100.0f));
            mMediaPlayer.start();
        }
    }

    private void initID() {
        eqLinearLayout = (LinearLayout) findViewById(R.id.linearlayout_eqband);
        timeSpectrum = (VisualizerView) findViewById(R.id.time_spectrum);
        selectFileButton = (Button) findViewById(R.id.btn_select);
        playButton = (Button) findViewById(R.id.btn_play);
        stopButton = (Button) findViewById(R.id.btn_stop);
        volume = (SeekBar) findViewById(R.id.seekbar_volume);
//       graph = (GraphView) findViewById(R.id.graph);
        bar = (BarVisualizer) findViewById(R.id.bar_spectrum);
    }

    private void onClickSelectButton() {
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(Intent.createChooser(intent, "Select File"), AppConstants.SELECTED_FILE_REQUEST_CODE);
            }
        });
    }

    private void onClickPlayButton() {
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.seekTo(audioCurrentPosition);
                        mMediaPlayer.start();
                    }
                }
                else{
                    Toast.makeText(v.getContext(),"Can't find your audio sound",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onClickStopButton() {
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    audioCurrentPosition = mMediaPlayer.getCurrentPosition();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.SELECTED_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.getData() != null)) {
                Uri uri = data.getData(); // GET URI FROM SELECTED FILE
                String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();   //GET DIRECTORY FROM "PHONE DIRECTORY"
                String fileName = getFileName(uri);  // GET FILE NAME FROM URI
                String path = baseDir + "/soundtest/" + fileName;    // MAKE FILE PATH

                if (mMediaPlayer == null) {
                    mMediaPlayer = new MediaPlayer();
                    initAudio(data.getData());

                } else {
                    mVisualizer.release();
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                    initAudio(data.getData());
                }
            }
        }
    }

    private void setUpTimeSpectum() {
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyhavePermission()) {
                requestForSpecificPermission();
            }
        }
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] bytes, int samplingRate) {
                        if(bytes != null){
                            Log.d("wavedatalength",String.valueOf(bytes.length));
                            for(int i   = 0;i<bytes.length;i++){
                                Log.d("wavedatacontent",String.valueOf(bytes[i]));
                            }
                        }
                        else{
                            Log.d("wavedatalength","NULL DATA");
                        }
                            timeSpectrum.updateVisualizer(bytes);



                    }

                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] bytes, int samplingRate) {
                        if(bytes != null){
                            bar.setRawAudioBytes(bytes);
                            bar.show();
                            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    if (bar != null)
                                        bar.hide();
                                }
                            });
                        }
                        else{
                            Log.d("fftdatalength","NULL DATA");
                        }

                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true);

    }

    private void setupEqualizerBands() {
        mEqualizer = new Equalizer(0, 0);
        mEqualizer.setEnabled(true);
        short bands = mEqualizer.getNumberOfBands();    /////5 number of bands (60000 .. 230000 .. 910000 .. 3600000 ..14000000) mHz
        verticalSeekBars = new ArrayList<VerticalSeekBar>();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1;

        LinearLayout newOne = new LinearLayout(this);
        newOne.setLayoutParams(params);
        newOne.setOrientation(LinearLayout.HORIZONTAL);

        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

        for (short i = 0; i < bands; i++) {
            final short band = i;

            LinearLayout row = new LinearLayout(this);

            row.setOrientation(LinearLayout.VERTICAL);

            TextView minDbTextView = new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minEQLevel / 100) + " dB");
            minDbTextView.setTextColor(0xff000000);

            TextView maxDbTextView = new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxEQLevel / 100) + " dB");
            maxDbTextView.setTextColor(0xff000000);

            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
            freqTextView.setPadding(0, 20, 50, 0);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 500);
            layoutParams.setMargins(0, 80, 80, 80);

//--------------------------

            // setClassicTone(maxEQLevel, minEQLevel);

            VerticalSeekBar bar = new VerticalSeekBar(this);

            bar = new VerticalSeekBar(this);
            bar.setLayoutParams(layoutParams);
//            bar.setMax(maxEQLevel - minEQLevel);
            bar.setMax(3000);
            bar.setProgress(mEqualizer.getBandLevel(band)+1500);
            bar.setPadding(20, 20, 20, 20);

            //bar.setProgressDrawable(R.drawable.scrubber_progress_horizontal_holo_light);
            //System.out.println("Progress:::"+(mEqualizer.getBandLevel(band)));
            //bar[i].setProgress((maxEQLevel-minEQLevel)/2);
            //System.out.println("Presets are: "+mEqualizer.getNumberOfPresets()+" And name is:  "+mEqualizer.getPresetName(i));

            //mEqualizer.setBandLevel(band, level)
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
                    Log.d("SEEKBARCHANGE", String.valueOf(progress) + " " + String.valueOf(mEqualizer.getBandLevel(band)));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            verticalSeekBars.add(bar);
            row.addView(maxDbTextView);
            row.addView(bar);
            row.addView(minDbTextView);
            row.addView(freqTextView);
            newOne.addView(row, params);

        }

        eqLinearLayout.addView(newOne);
    }

    private void volumeSetup() {
        volume.setProgress(currentVolume);
        volume.setMax(100);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("SEEKBARCHANGE", "Seek Bar changed");
                currentVolume = progress;
                if(mMediaPlayer!=null){
                    mMediaPlayer.setVolume((currentVolume / 100.0f), (currentVolume / 100.0f));
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //CHECK IF ALREADY HAVE  PERMISSION
    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    //REQUEST PERMISSION
    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /////GET FILE NAME FROM URI
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.presets:
                PresetDialog presetDialog = new PresetDialog();
                presetDialog.show(getSupportFragmentManager(), "presetDialog");
                return true;
            case R.id.freq_spectrum:
                    changeSpectrum();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    public void changeSpectrum(){
        spectrumChanged = !spectrumChanged;
        if(spectrumChanged == false){
            timeSpectrum.setVisibility(View.VISIBLE);
            bar.setVisibility(View.INVISIBLE);
        }
        else{
            timeSpectrum.setVisibility(View.INVISIBLE);
            bar.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onReturnCheckedPosition(int checkPosition) {
        Log.d("selected",String.valueOf(checkPosition));
        usePresetAndSetSeekBarLevels(checkPosition);
    }
    public void usePresetAndSetSeekBarLevels(int checkedPosition){
        mEqualizer.usePreset((short) checkedPosition); //USE PRESET FROM checkedPosition
        short bands = mEqualizer.getNumberOfBands();
        for (short i = 0; i < bands; i++) {
            short band = (short) i;
            verticalSeekBars.get(i).setProgress(mEqualizer.getBandLevel(band)+1500);
            Log.d("SEEKBARCHANGE", String.valueOf(mEqualizer.getBandLevel(band)+1500));

        }

    }
}
