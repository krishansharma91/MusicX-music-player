package com.rks.musicx.ui.fragments;


import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.appthemeengine.Config;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cleveroad.audiowidget.SmallBang;
import com.cleveroad.play_widget.PlayLayout;
import com.cleveroad.play_widget.VisualizerShadowChanger;
import com.rks.musicx.R;
import com.rks.musicx.base.BasePlayingFragment;
import com.rks.musicx.base.BaseRecyclerViewAdapter;
import com.rks.musicx.data.model.Song;
import com.rks.musicx.database.FavHelper;
import com.rks.musicx.interfaces.bitmap;
import com.rks.musicx.interfaces.palette;
import com.rks.musicx.misc.utils.ArtworkUtils;
import com.rks.musicx.misc.utils.CustomLayoutManager;
import com.rks.musicx.misc.utils.Extras;
import com.rks.musicx.misc.utils.GestureListerner;
import com.rks.musicx.misc.utils.Helper;
import com.rks.musicx.misc.utils.PlayingPagerAdapter;
import com.rks.musicx.misc.utils.SimpleItemTouchHelperCallback;
import com.rks.musicx.misc.utils.permissionManager;
import com.rks.musicx.misc.widgets.changeAlbumArt;
import com.rks.musicx.misc.widgets.updateAlbumArt;
import com.rks.musicx.services.MediaPlayerSingleton;
import com.rks.musicx.ui.adapters.QueueAdapter;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static com.rks.musicx.R.id.song_artist;
import static com.rks.musicx.R.id.song_title;


/*
 * Created by Coolalien on 6/28/2016.
 */

/*
 * ©2017 Rajneesh Singh
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Playing2Fragment extends BasePlayingFragment implements SimpleItemTouchHelperCallback.OnStartDragListener {

    private Handler mHandler = new Handler();
    private PlayLayout mPlayLayout;
    private ImageView blur_artowrk;
    private TextView songTitle, songArtist;
    private TextView lrcView;
    private RecyclerView queuerv;
    private QueueAdapter queueAdapter;
    private String ateKey;
    private int accentColor, pos, duration;
    private VisualizerShadowChanger visualizerShadowChanger;
    private FavHelper favhelper;
    private SmallBang mSmallBang;
    private ImageButton favButton, moreMenu;
    private ViewPager Pager;
    private PlayingPagerAdapter PlayingPagerAdapter;
    private List<View> Playing4PagerDetails;
    private ItemTouchHelper mItemTouchHelper;
    private List<Song> queueList = new ArrayList<>();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (getMusicXService() != null && getMusicXService().isPlaying()) {
                pos = getMusicXService().getPlayerPos();
                duration = getMusicXService().getDuration();
                mPlayLayout.setPostProgress((float) pos / duration);
                mPlayLayout.getCurrent().setText(Helper.durationCalculator(pos));
            }
            mHandler.postDelayed(runnable, 1000);
        }
    };

    private BaseRecyclerViewAdapter.OnItemClickListener onClick = new BaseRecyclerViewAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            if (getMusicXService() == null) {
                return;
            }
            queuerv.scrollToPosition(position);
            switch (view.getId()) {
                case R.id.item_view:
                    getMusicXService().setdataPos(position, true);
                    Extras.getInstance().saveSeekServices(0);
                    break;
            }
        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (getMusicXService() == null) {
                return;
            }
            switch (view.getId()) {
                case R.id.action_favorite:
                    ImageButton button = (ImageButton) view;
                    if (favhelper.isFavorite(getMusicXService().getsongId())) {
                        favhelper.removeFromFavorites(getMusicXService().getsongId());
                        button.setImageResource(R.drawable.ic_action_favorite_outline);
                        int outlinecolor = ContextCompat.getColor(getContext(), R.color.white);
                        button.setColorFilter(outlinecolor);
                    } else {
                        favhelper.addFavorite(getMusicXService().getsongId());
                        button.setImageResource(R.drawable.ic_action_favorite);
                        button.setColorFilter(accentColor);
                        like(view);
                    }
                    break;
                case R.id.menu_button:
                    playingMenu(queueAdapter, view, true);
                    break;
            }
        }
    };


    private void coverArtView() {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArtworkUtils.ArtworkLoader(getContext(), getMusicXService().getsongAlbumName(), null, getMusicXService().getsongAlbumID(), new palette() {
                    @Override
                    public void palettework(Palette palette) {
                        final int[] colors = Helper.getAvailableColor(getContext(), palette);
                        if(getActivity().getWindow() == null){
                            return;
                        }
                        if (Extras.getInstance().getDarkTheme() || Extras.getInstance().getBlackTheme()) {
                            getActivity().getWindow().setStatusBarColor(colors[0]);
                        } else {
                            getActivity().getWindow().setStatusBarColor(colors[0]);
                        }
                        if (Extras.getInstance().artworkColor()) {
                            colorMode(colors[0]);
                        } else {
                            colorMode(accentColor);
                        }
                    }
                }, new bitmap() {
                    @Override
                    public void bitmapwork(Bitmap bitmap) {
                        mPlayLayout.setImageBitmap(bitmap);
                        ArtworkUtils.blurPreferances(getContext(), bitmap, blur_artowrk);
                    }

                    @Override
                    public void bitmapfailed(Bitmap bitmap) {
                        mPlayLayout.setImageBitmap(bitmap);
                        ArtworkUtils.blurPreferances(getContext(), bitmap, blur_artowrk);
                    }
                });
            }
        });
        isalbumArtChanged = true;
    }

    @Override
    protected void reload() {
        playingView();
        updateRepeatButton();
        updateShuffleButton();
        updatePlaylayout();
        if (isalbumArtChanged) {
            coverArtView();
            isalbumArtChanged = false;
        } else {
            ChangeAlbumCover(getImagePath());
            isalbumArtChanged = true;
        }
        mHandler.post(runnable);
    }

    @Override
    protected void playbackConfig() {
        updatePlaylayout();
    }

    @Override
    protected void metaConfig() {
        playingView();
        mHandler.post(runnable);
        if (isalbumArtChanged) {
            coverArtView();
            isalbumArtChanged = false;
        } else {
            ChangeAlbumCover(getImagePath());
            isalbumArtChanged = true;
        }
    }

    @Override
    protected void queueConfig() {
        updateQueue();
    }

    @Override
    protected void onPaused() {
        if (visualizerShadowChanger != null) {
            visualizerShadowChanger.setEnabledVisualization(false);
        }
    }


    @Override
    protected void ui(View rootView) {
        blur_artowrk = (ImageView) rootView.findViewById(R.id.blur_artwork);
        Pager = (ViewPager) rootView.findViewById(R.id.pagerPlaying4);
        songTitle = (TextView) rootView.findViewById(song_title);
        songArtist = (TextView) rootView.findViewById(song_artist);
        moreMenu = (ImageButton) rootView.findViewById(R.id.menu_button);
        favButton = (ImageButton) rootView.findViewById(R.id.action_favorite);
        queuerv = (RecyclerView) rootView.findViewById(R.id.commonrv);

        View coverView = LayoutInflater.from(getContext()).inflate(R.layout.playing2_coverview, null);
        View lyricsView = LayoutInflater.from(getContext()).inflate(R.layout.lyricsview, null);

        mPlayLayout = (PlayLayout) coverView.findViewById(R.id.revealView);
        lrcView = (TextView) lyricsView.findViewById(R.id.lyrics);

        coverView.setOnTouchListener(new GestureListerner() {
            @Override
            public void onRightToLeft() {

            }

            @Override
            public void onLeftToRight() {

            }

            @Override
            public void onBottomToTop() {
            }

            @Override
            public void onTopToBottom() {
            }

            @Override
            public void doubleClick() {
                if(getActivity() == null){
                    return;
                }
                getActivity().onBackPressed();
            }
        });

        Playing4PagerDetails = new ArrayList<>(2);
        Playing4PagerDetails.add(coverView);
        Playing4PagerDetails.add(lyricsView);
        PlayingPagerAdapter = new PlayingPagerAdapter(Playing4PagerDetails);
        Pager.setAdapter(PlayingPagerAdapter);
    }

    @Override
    protected void function() {
        mPlayLayout.fastOpen();
        mPlayLayout.getIvSkipNext().setImageResource(R.drawable.aw_ic_next);
        mPlayLayout.getIvSkipPrevious().setImageResource(R.drawable.aw_ic_prev);
        ateKey = Helper.getATEKey(getContext());
        accentColor = Config.accentColor(getContext(), ateKey);
        mPlayLayout.setPlayButtonBackgroundTintList(ColorStateList.valueOf(accentColor));
        moreMenu.setOnClickListener(mOnClickListener);
        moreMenu.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_menu));
        favhelper = new FavHelper(getContext());
        mSmallBang = SmallBang.attach2Window(getActivity());
        favButton.setOnClickListener(mOnClickListener);
        startVisualiser();
        CustomLayoutManager customlayoutmanager = new CustomLayoutManager(getActivity());
        customlayoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
        queuerv.setLayoutManager(customlayoutmanager);
        queuerv.setHasFixedSize(true);
        queueAdapter = new QueueAdapter(getContext(), this);
        queueAdapter.setLayoutId(R.layout.gridqueue);
        queuerv.setAdapter(queueAdapter);
        queueAdapter.setOnItemClickListener(onClick);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(queueAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(queuerv);
        if (Extras.getInstance().mPreferences.getBoolean("dark_theme", false)) {
            mPlayLayout.setProgressLineColor(ContextCompat.getColor(getContext(), R.color.translucent_white_8p));
        } else {
            mPlayLayout.setProgressLineColor(ContextCompat.getColor(getContext(), R.color.translucent_white_8p));
        }
        if (getActivity() == null || getActivity().getWindow() == null){
            return;
        }
        getActivity().getWindow().setStatusBarColor(accentColor);
        /**
         * Show Case
         */
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500);
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "400");
        sequence.setConfig(config);
        sequence.addSequenceItem(queuerv, "Slide right/left to view Lyrics/PlayingView", "GOT IT");
        sequence.setOnItemDismissedListener(new MaterialShowcaseSequence.OnSequenceItemDismissedListener() {
            @Override
            public void onDismiss(MaterialShowcaseView materialShowcaseView, int i) {
                materialShowcaseView.hide();
            }
        });
        sequence.start();
    }

    @Override
    protected int setLayout() {
        return R.layout.fragment_playing2;
    }

    @Override
    protected void playingView() {
        if (getMusicXService() != null) {
            String title = getMusicXService().getsongTitle();
            String artist = getMusicXService().getsongArtistName();
            songTitle.setText(title);
            songTitle.setSelected(true);
            songTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            songArtist.setText(artist);
            songArtist.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            isalbumArtChanged = true;
            mPlayLayout.setOnButtonsClickListener(new PlayLayout.OnButtonsClickListenerAdapter() {
                @Override
                public void onPlayButtonClicked() {
                    playpauseclicked();
                }

                @Override
                public void onSkipPreviousClicked() {
                    getMusicXService().playprev(true);
                    if (!mPlayLayout.isOpen()) {
                        mPlayLayout.startRevealAnimation();
                    }
                }

                @Override
                public void onSkipNextClicked() {
                    getMusicXService().playnext(true);
                    if (!mPlayLayout.isOpen()) {
                        mPlayLayout.startRevealAnimation();
                    }
                }

                @Override
                public void onShuffleClicked() {
                    boolean shuffle = getMusicXService().isShuffleEnabled();
                    getMusicXService().setShuffleEnabled(!shuffle);
                    updateShuffleButton();
                }

                @Override
                public void onRepeatClicked() {
                    int mode = getMusicXService().getNextrepeatMode();
                    getMusicXService().setRepeatMode(mode);
                    updateRepeatButton();
                }
            });
            mPlayLayout.setOnProgressChangedListener(new PlayLayout.OnProgressChangedListener() {
                @Override
                public void onPreSetProgress() {
                    if (getMusicXService() != null) {
                        try {
                            mHandler.removeCallbacks(runnable);
                        } catch (Exception c){
                            c.printStackTrace();
                        }finally {
                            mHandler.post(runnable);
                        }
                    }
                }

                @Override
                public void onProgressChanged(float progress) {
                    if (getMusicXService() != null) {
                        int dur = getMusicXService().getDuration();
                        if (dur != -1) {
                            getMusicXService().seekto((int) (dur * progress));
                        }
                    }
                }

            });
            updateQueue();
            if (favhelper.isFavorite(getMusicXService().getsongId())) {
                favButton.setImageResource(R.drawable.ic_action_favorite);
            } else {
                favButton.setImageResource(R.drawable.ic_action_favorite_outline);
            }
            int dur = getMusicXService().getDuration();
            if (dur != -1) {
                mPlayLayout.getDur().setText(Helper.durationCalculator(dur));
            }
            new Helper(getContext()).LoadLyrics(title, artist, getMusicXService().getsongData(), lrcView);
        }
    }

    @Override
    protected ImageView shuffleButton() {
        return mPlayLayout.getIvShuffle();
    }

    @Override
    protected ImageView repeatButton() {
        return mPlayLayout.getIvRepeat();
    }

    private void startVisualiser() {
        if (permissionManager.isAudioRecordGranted(getContext())) {
            visualizerShadowChanger = VisualizerShadowChanger.newInstance(MediaPlayerSingleton.getInstance().getMediaPlayer().getAudioSessionId());
            visualizerShadowChanger.setEnabledVisualization(true);
            mPlayLayout.setShadowProvider(visualizerShadowChanger);
            Log.i("startVisualiser", "startVisualiser " + MediaPlayerSingleton.getInstance().getMediaPlayer().getAudioSessionId());
        } else {
            Log.d("PlayingFragment2", "Permission not granted");
        }

    }

    public void like(View view) {
        favButton.setImageResource(R.drawable.ic_action_favorite);
        mSmallBang.bang(view);
        mSmallBang.setmListener(new SmallBang.SmallBangListener() {
            @Override
            public void onAnimationStart() {
            }

            @Override
            public void onAnimationEnd() {
            }
        });
    }

    private void updateQueue() {
        if (getMusicXService() == null) {
            return;
        }
        queueList = getMusicXService().getPlayList();
        if (queueList != queueAdapter.getSnapshot() && queueList.size() > 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    queueAdapter.addDataList(queueList);
                }
            });
        }
        queueAdapter.setSelection(getMusicXService().returnpos());
        if (getMusicXService().returnpos() >=0 && getMusicXService().returnpos() < queueAdapter.getSnapshot().size()){
            queuerv.scrollToPosition(getMusicXService().returnpos());
        }
    }

    private void updatePlaylayout() {
        if (!getMusicXService().isPlaying()) {
            if (mPlayLayout.isOpen()) {
                mPlayLayout.startDismissAnimation();
            }
        } else {
            if (!mPlayLayout.isOpen()) {
                mPlayLayout.startRevealAnimation();
            }
        }
    }

    private void colorMode(int color) {
        if (getActivity() == null || getActivity().getWindow() == null) {
            return;
        }
        if (Extras.getInstance().getDarkTheme() || Extras.getInstance().getBlackTheme()) {
            getActivity().getWindow().setNavigationBarColor(color);
            mPlayLayout.setBigDiffuserColor(Helper.getColorWithAplha(color, 0.3f));
            mPlayLayout.setMediumDiffuserColor(Helper.getColorWithAplha(color, 0.4f));
            mPlayLayout.getPlayButton().setBackgroundTintList(ColorStateList.valueOf(accentColor));
            mPlayLayout.setProgressBallColor(color);
            mPlayLayout.setProgressCompleteColor(color);
            getActivity().getWindow().setStatusBarColor(color);
        } else {
            getActivity().getWindow().setNavigationBarColor(color);
            mPlayLayout.setBigDiffuserColor(Helper.getColorWithAplha(color, 0.3f));
            mPlayLayout.setMediumDiffuserColor(Helper.getColorWithAplha(color, 0.4f));
            mPlayLayout.getPlayButton().setBackgroundTintList(ColorStateList.valueOf(accentColor));
            mPlayLayout.setProgressBallColor(color);
            mPlayLayout.setProgressCompleteColor(color);
            getActivity().getWindow().setStatusBarColor(color);
        }

    }

    private void playpauseclicked() {
        if (mPlayLayout == null) {
            return;
        }
        if (mPlayLayout.isOpen()) {
            getMusicXService().toggle();
            mPlayLayout.startDismissAnimation();
        } else {
            getMusicXService().toggle();
            mPlayLayout.startRevealAnimation();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (visualizerShadowChanger != null) {
            visualizerShadowChanger.release();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (visualizerShadowChanger != null) {
            visualizerShadowChanger.setEnabledVisualization(true);
        }
    }


    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    protected void changeArtwork() {
        ChangeAlbumCover(getImagePath());
    }

    @Override
    protected TextView lyricsView() {
        return lrcView;
    }

    private void ChangeAlbumCover(String finalPath) {
        if (getMusicXService() != null) {
            if (getChosenImages() != null) {
                new updateAlbumArt(finalPath, getMusicXService().getsongData(), getContext(), getMusicXService().getsongAlbumID(), new changeAlbumArt() {
                    @Override
                    public void onPostWork() {
                        ArtworkUtils.ArtworkLoader(getContext(), getMusicXService().getsongAlbumName(), finalPath, getMusicXService().getsongAlbumID(), new palette() {
                            @Override
                            public void palettework(Palette palette) {
                                final int[] colors = Helper.getAvailableColor(getContext(), palette);
                                if(getActivity().getWindow() == null || getActivity() == null){
                                    return;
                                }
                                if (Extras.getInstance().getDarkTheme() || Extras.getInstance().getBlackTheme()) {
                                    getActivity().getWindow().setStatusBarColor(colors[0]);
                                } else {
                                    getActivity().getWindow().setStatusBarColor(colors[0]);
                                }
                                if (Extras.getInstance().artworkColor()) {
                                    colorMode(colors[0]);
                                } else {
                                    colorMode(accentColor);
                                }
                            }
                        }, new bitmap() {
                            @Override
                            public void bitmapwork(Bitmap bitmap) {
                                ArtworkUtils.blurPreferances(getContext(), bitmap, blur_artowrk);
                                mPlayLayout.setImageBitmap(bitmap);
                            }

                            @Override
                            public void bitmapfailed(Bitmap bitmap) {
                                ArtworkUtils.blurPreferances(getContext(), bitmap, blur_artowrk);
                                mPlayLayout.setImageBitmap(bitmap);
                            }
                        });
                        queueAdapter.notifyDataSetChanged();
                    }
                }).execute();

                if (permissionManager.isAudioRecordGranted(getContext())){
                    Glide.with(getContext())
                            .load(finalPath)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .format(DecodeFormat.PREFER_ARGB_8888)
                            .override(getSize(), getSize())
                            .transform(new CropCircleTransformation(getContext()))
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onLoadStarted(Drawable placeholder) {
                                }

                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    if (getMusicXService().getAudioWidget() != null){
                                        getMusicXService().getAudioWidget().controller().albumCoverBitmap(ArtworkUtils.drawableToBitmap(errorDrawable));
                                    }
                                }

                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    if (getMusicXService().getAudioWidget() != null){
                                        getMusicXService().getAudioWidget().controller().albumCoverBitmap(resource);
                                    }
                                }

                            });
                }
            }
        }
    }

}
