package com.lixiangsoft.lixiang.music_player;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.AestheticActivity;
import com.afollestad.aesthetic.TabLayoutBgMode;
import com.afollestad.aesthetic.TabLayoutIndicatorMode;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gyf.barlibrary.ImmersionBar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.wcy.lrcview.LrcView;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.COLLAPSED;
import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.DRAGGING;
import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.EXPANDED;

@RuntimePermissions
public class MainActivity extends AestheticActivity {
    private static SeekBar seekBar;
    private boolean isFirstTime = true;
    private ViewPager viewPager;
    private MsgReceiver msgReceiver;
    private boolean isfromSc = false;
    private int flag = 0;
    private int cx = 0;
    private int cy = 0;
    private int finalRadius = 0;
    private PlayService playService;
    private Timer mTimer;
    private NavigationView navigationView;
    private FloatingActionButton random_play;
    private SlidingUpPanelLayout mLayout;
    private ImageView lunch_play_now_button;
    private ImageView play_pause_button;
    private ImageView back;
    private ImageView about;
    private List<Music> lyricList;
    private LrcView otherLyricView;
    private boolean fromLyric = false;
    private Dialog dialog;
    boolean visible;
    private TransitionSet set = new TransitionSet()
//                                .addTransition(new Scale(0.7f))
            .addTransition(new Fade())
            .setInterpolator(visible ? new LinearOutSlowInInterpolator() :
                    new FastOutLinearInInterpolator());
    private ViewGroup transitionsContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //沉浸状态栏
        ImmersionBar.with(MainActivity.this).statusBarView(R.id.immersion_view).init();

        if (Aesthetic.isFirstTime()) {
            Aesthetic.get()
                    .colorPrimaryRes(R.color.colorPrimary)
                    .colorAccentRes(R.color.colorAccent)
                    .colorCardViewBackgroundRes(R.color.cardview_light_background)
                    .colorStatusBarAuto()
                    .colorNavigationBarAuto()
                    .apply();
        }

        //初始化全局变量
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        random_play = (FloatingActionButton) findViewById(R.id.random_play);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        lunch_play_now_button = (ImageView) findViewById(R.id.lunch_play_now_button);
        play_pause_button = (ImageView) findViewById(R.id.play_pause_button);
        back = (ImageView) findViewById(R.id.back);
        about = (ImageView) findViewById(R.id.about);
        otherLyricView = (LrcView) findViewById(R.id.other_lrc_view);
        transitionsContainer = (ViewGroup) findViewById(R.id.activity_now_play);


        //多屏幕尺寸适应
        new screenAdaptionTask().execute();

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        SimpleFragmentPagerAdapter adapter = new SimpleFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                if (position == 0) {
                    if (mLayout.getPanelHeight() == 0) {
                        random_play.show();
                    }
                    navigationView.setCheckedItem(R.id.suggestion_view);
                } else if (position == 1) {
                    if (mLayout.getPanelHeight() == 0) {
                        random_play.show();
                    }
                    navigationView.setCheckedItem(R.id.music_list_view);
                } else if (position == 3) {
                    if (mLayout.getPanelHeight() == 0) {
                        random_play.show();
                    }
                    navigationView.setCheckedItem(R.id.custom_list_view);
                } else {
                    random_play.hide();
                    navigationView.setCheckedItem(R.id.download_view);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        //新标题栏
        Toolbar main_toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        main_toolbar.inflateMenu(R.menu.main_menu);
        setSupportActionBar(main_toolbar);

        //侧滑栏动画
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, main_toolbar, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                View navHeader = inflater.inflate(R.layout.nav_header, (ViewGroup) MainActivity.this.findViewById(R.id.nav_header));
                ImageView imageView = (ImageView) navHeader.findViewById(R.id.header);
                if (sharedPref.getString("main_theme", "day").equals("day")) {
                    Glide.with(MainActivity.this).load(R.drawable.nav).into(imageView);
                } else {
                    Glide.with(MainActivity.this).load(R.drawable.nav_black).into(imageView);
                }
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                switch (flag) {
                    case 0:
                        break;
                    case R.id.nav_settings:
                        Intent settings_intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settings_intent);
                        flag = 0;
                        break;
                    case R.id.nav_about:
                        Intent about_intent = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(about_intent);
                        flag = 0;
                        break;
                    default:
                }
            }
        };
        mDrawerToggle.syncState();
        drawerLayout.setDrawerListener(mDrawerToggle);
        //Nac_view点击
//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.suggestion_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.suggestion_view) {
                    viewPager.setCurrentItem(0);
                } else if (id == R.id.music_list_view) {
                    viewPager.setCurrentItem(1);
                } else if (id == R.id.download_view) {
                    viewPager.setCurrentItem(2);
                } else if (id == R.id.custom_list_view) {
                    viewPager.setCurrentItem(3);
                } else if (id == R.id.nav_settings) {
                    flag = id;
                } else if (id == R.id.nav_about) {
                    flag = id;
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });


        //动态注册广播
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("play_broadcast");
        registerReceiver(msgReceiver, intentFilter);

        //上滑面板
        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                CardView main_control_ui = (CardView) findViewById(R.id.main_control_ui);
                AlphaAnimation alphaAnimation = new AlphaAnimation(slideOffset, 1 - slideOffset);
                main_control_ui.startAnimation(alphaAnimation);
                alphaAnimation.setFillAfter(true);//动画结束后保持状态
                alphaAnimation.setDuration(0);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (previousState == COLLAPSED && newState == DRAGGING) {
//                    if (playService != null) {
//                        seekBar.setMax(MyApplication.getMediaDuration());
//                    }
                    updateSeekBar();
                }
                if (previousState == DRAGGING && newState == EXPANDED) {
                    //禁止手势滑动
                    play_pause_button.setClickable(false);
                    lunch_play_now_button.setClickable(false);
                    about.setClickable(true);
                    back.setClickable(true);
                    final ImageView play_now_cover = (ImageView) findViewById(R.id.play_now_cover);

                    play_now_cover.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!otherLyricView.hasLrc()) {
                                //判断是否有用户自己储存歌词
                                musicInfo musicNow = MyApplication.getMusicListNow().get(MyApplication.getPositionNow());
                                String path = musicNow.getMusicData();
                                int total = path.length();
                                File file = new File(path.substring(0, total - 4) + ".lrc");
                                //判断是否有程序储存歌词
//                                File file2 = new File(MainActivity.this.getFilesDir().getAbsolutePath()+MyApplication.getMusicListNow().get(MyApplication.getPositionNow()).getMusicTitle());
                                if (file.isFile() && file.exists()) {
                                    //加载歌词
                                    otherLyricView.loadLrc(file);
                                    changeVisibility();
                                    otherLyricView.setOnPlayClickListener(new LrcView.OnPlayClickListener() {
                                        @Override
                                        public boolean onPlayClick(long time) {
                                            fromLyric = true;
                                            seekBar.setProgress((int) time);
                                            return false;
                                        }
                                    });
                                }
//                                else if(file2.isFile() && file2.exists()){
//                                    //加载歌词
//                                    otherLyricView.loadLrc(file2);
//                                    changeVisibility();
//                                    otherLyricView.setOnPlayClickListener(new LrcView.OnPlayClickListener() {
//                                        @Override
//                                        public boolean onPlayClick(long time) {
//                                            fromLyric = true;
//                                            seekBar.setProgress((int) time);
//                                            return false;
//                                        }
//                                    });
//                                }
                                else if (MyApplication.getLocal_net_mode() == false) {
                                    new getLyricTask().execute(musicNow.getMusicTitle(), musicNow.getMusicArtist());
                                    changeVisibility();
                                } else {
                                    Toast.makeText(MainActivity.this, "当前处于离线模式", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                changeVisibility();
                            }
                        }
                    });
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
                if (previousState == DRAGGING && newState == COLLAPSED) {
                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                    play_pause_button.setClickable(true);
                    lunch_play_now_button.setClickable(true);
                    about.setClickable(false);
                    back.setClickable(false);
                    //恢复手势滑动
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                }
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLayout != null &&
                        (mLayout.getPanelState() == EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
                    mLayout.setPanelState(COLLAPSED);
                }
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
                popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
                popupMenu.show();
                MenuItem search = popupMenu.getMenu().findItem(R.id.search);
                MenuItem sleeper = popupMenu.getMenu().findItem(R.id.sleeper);
                MenuItem equalizer = popupMenu.getMenu().findItem(R.id.equalizer);
                search.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent intent = new Intent(MainActivity.this, searchActivity.class);
                        startActivity(intent);
                        return true;
                    }
                });
                sleeper.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        //Timepicker Dialog
                        sleeper();
                        return true;
                    }
                });
                equalizer.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        equalizer();
                        return false;
                    }
                });
            }
        });


        lunch_play_now_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(EXPANDED);
            }
        });


        if (MyApplication.getState() == MyConstant.playing) {
            ChangeScrollingUpPanel(MyApplication.getPositionNow());
            random_play.hide();
            mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
            mLayout.setPanelHeight((int) (60 * getResources().getDisplayMetrics().density + 0.5f));
        }
        MainActivityPermissionsDispatcher.needsPermissionWithCheck(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem search = menu.findItem(R.id.search);
        MenuItem sleeper = menu.findItem(R.id.sleeper);
        MenuItem equalizer = menu.findItem(R.id.equalizer);
        search.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(MainActivity.this, searchActivity.class);
                startActivity(intent);
                return true;
            }
        });
        sleeper.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //Timepicker Dialog
                sleeper();
                return true;
            }
        });
        equalizer.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                equalizer();
                return false;
            }
        });


        return true;
    }


    @Override
    protected void onStart() {
        Log.e("OnStart执行", "OnStart");
        super.onStart();
        ensureServiceStarted();
        //绑定服务
        Intent bindIntent = new Intent(this, PlayService.class);
        bindService(bindIntent, conn, BIND_AUTO_CREATE);

        //拖动seekbar
        seekBar.setPadding(5, 0, 5, 0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (playService != null) {
                    if (fromUser || fromLyric) {
                        playService.seekto(progress);
                        fromLyric = false;
                    }
                    if (MyApplication.getState() == MyConstant.playing) {
                        otherLyricView.updateTime(playService.getCurrentPosition());
                    }
                }
                //更新Seekbar的时间显示
                TextView currentPosition = (TextView) findViewById(R.id.current_position);
                currentPosition.setText(toTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //设置起始页
        if (isfromSc == false) {
            if (isFirstTime) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String start_page = sharedPref.getString("start_page", "");
                switch (start_page) {
                    case "suggestion":
                        viewPager.setCurrentItem(0);
                        break;
                    case "list":
                        viewPager.setCurrentItem(1);
                        break;
                    case "cloud":
                        viewPager.setCurrentItem(2);
                        break;
                    default:
                }
                isFirstTime = false;
            }
        }

        random_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication.setPlayMode(MyConstant.random);
                MyApplication.setMusicListNow(MyApplication.getMusicInfoArrayList(), "musicInfoArrayList");
                Intent intent = new Intent("service_broadcast");
                intent.putExtra("ACTION", MyConstant.playAction);
                sendBroadcast(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mLayout != null &&
                (mLayout.getPanelState() == EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mLayout.setPanelState(COLLAPSED);
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(msgReceiver);
        Log.v("OnDestory执行", "OnDestory");
        if (MyApplication.getState() == MyConstant.pausing) {
            unbindService(conn);
            stopService(new Intent(this, PlayService.class));
        }
        super.onDestroy();
    }
    //以下为公共方法

    public void sendPermissionGranted() {
        new initialTask().execute();
    }

    public class initialTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            MyApplication.initialMusicInfo(MainActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (MyApplication.hasInitialized) {
                //其他界面初始化列表
                Intent intent = new Intent("permission_granted");
                sendBroadcast(intent);
                Intent intent2 = new Intent("list_permission_granted");
                sendBroadcast(intent2);
            }
            super.onPostExecute(o);
        }
    }

    //播放，暂停按钮
    public void title_play_or_pause(View view) {
        if (MyApplication.getState() == MyConstant.playing) {
            playService.pause();
            playService.removeAudioFocus();
        } else if (MyApplication.getState() == MyConstant.pausing) {
            playService.resume();
        }
    }

    public void main_play_or_pause(View v) {
        if (MyApplication.getState() == MyConstant.playing) {
            playService.pause();
            playService.removeAudioFocus();
        } else if (MyApplication.getState() == MyConstant.pausing) {
            playService.resume();
        }
    }

    public void previous(View v) {
        playService.previous();
    }

    public void next(View v) {
        playService.next();
    }

    public void changeRepeat(View v) {
        //playMode 0:列表重复 1:随机 2:单曲重复 3:顺序
        ImageView repeat_button = (ImageView) findViewById(R.id.repeat_button);
        ImageView shuffle_button = (ImageView) findViewById(R.id.shuffle_button);

        switch (MyApplication.getPlayMode()) {
            case MyConstant.list:
                MyApplication.setPlayMode(MyConstant.list_repeat);
                Snackbar.make(mLayout, "列表循环播放", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                }).show();
                repeat_button.setImageResource(R.drawable.repeat);
                shuffle_button.setImageResource(R.drawable.shuffle_grey);
                break;
            case MyConstant.list_repeat:
                MyApplication.setPlayMode(MyConstant.one_repeat);
                Snackbar.make(mLayout, "单曲循环播放", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                }).show();
                repeat_button.setImageResource(R.drawable.repeat_one);
                shuffle_button.setImageResource(R.drawable.shuffle_grey);
                break;
            case MyConstant.one_repeat:
                MyApplication.setPlayMode(MyConstant.list);
                Snackbar.make(mLayout, "顺序播放", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                }).show();
                repeat_button.setImageResource(R.drawable.repeat_grey);
                shuffle_button.setImageResource(R.drawable.shuffle_grey);
                break;
            case MyConstant.random:
                MyApplication.setPlayMode(MyConstant.list_repeat);
                Snackbar.make(mLayout, "列表重复播放", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                }).show();
                shuffle_button.setImageResource(R.drawable.shuffle_grey);
                repeat_button.setImageResource(R.drawable.repeat);
                break;
            default:
        }
    }

    public void changeShuffle(View v) {
        ImageView repeat_button = (ImageView) findViewById(R.id.repeat_button);
        ImageView shuffle_button = (ImageView) findViewById(R.id.shuffle_button);
        if (MyApplication.getPlayMode() == MyConstant.random) {
            MyApplication.setPlayMode(MyConstant.list);
            Snackbar.make(mLayout, "顺序播放", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            }).show();
            shuffle_button.setImageResource(R.drawable.shuffle_grey);
            repeat_button.setImageResource(R.drawable.repeat_grey);
        } else {
            MyApplication.setPlayMode(MyConstant.random);
            Snackbar.make(mLayout, "随机播放", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            }).show();
            shuffle_button.setImageResource(R.drawable.shuffle);
            repeat_button.setImageResource(R.drawable.repeat_grey);
        }
    }

    public void animation_change_color(int Int) {
        ImageView play_now_back_color = (ImageView) findViewById(R.id.play_now_back_color);
        if (cx == 0) {
            FloatingActionButton play_or_pause = (FloatingActionButton) findViewById(R.id.play_or_pause);
            RelativeLayout seekbar_layout = (RelativeLayout) findViewById(R.id.seekbar_layout);
            RelativeLayout control_layout = (RelativeLayout) findViewById(R.id.control_layout);
            cx = play_or_pause.getLeft() + control_layout.getLeft() + play_or_pause.getWidth() / 2;
            cy = control_layout.getTop() - seekbar_layout.getTop() + play_or_pause.getTop() + play_or_pause.getHeight() / 2;
            finalRadius = Math.max(play_now_back_color.getWidth(), play_now_back_color.getHeight());
        }
        final int Int1 = Int;
        final RelativeLayout activity_now_play = (RelativeLayout) findViewById(R.id.activity_now_play);
        if (cx != 0) {
            Animator anim = ViewAnimationUtils.createCircularReveal(play_now_back_color, cx, cy, 0, finalRadius);
            play_now_back_color.setBackgroundColor(Int);
            anim.setDuration(500);
            anim.start();
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    activity_now_play.setBackgroundColor(Int1);
                }
            });
        } else {
            activity_now_play.setBackgroundColor(Int1);
        }
        TextView now_on_play_text = (TextView) findViewById(R.id.now_on_play_text);
        now_on_play_text.setTextColor(Int);
        //lrcview字体颜色

        //歌词背景颜色
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            View bottom = findViewById(R.id.gradient_bottom);
            View top = findViewById(R.id.gradient_top);
            View gradient = findViewById(R.id.gradient);
            top.setBackground(
                    ScrimUtil.makeCubicGradientScrimDrawable(Int1, //颜色
                            8, //渐变层数
                            Gravity.TOP)); //起始方向
            bottom.setBackground(
                    ScrimUtil.makeCubicGradientScrimDrawable(Int1, //颜色
                            8, //渐变层数
                            Gravity.BOTTOM)); //起始方向
            gradient.setBackground(
                    ScrimUtil.makeCubicGradientScrimDrawable(Int1, //颜色
                            8, //渐变层数
                            Gravity.BOTTOM)); //起始方向
        }
    }

    public void ChangeScrollingUpPanel(int position) {
        musicInfo nowMusic = MyApplication.getMusicListNow().get(position);
        String title = nowMusic.getMusicTitle();
        String artist = nowMusic.getMusicArtist();
        seekBar.setProgress(0);
        seekBar.setMax(MyApplication.getMediaDuration());
        final TextView duration = (TextView) findViewById(R.id.duration);
        duration.setText(toTime(MyApplication.getMediaDuration()));
        TextView main_song_title = (TextView) findViewById(R.id.main_song_title);
        final ImageView repeat_button = (ImageView) findViewById(R.id.repeat_button);
        ImageView shuffle_button = (ImageView) findViewById(R.id.shuffle_button);
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.play_or_pause);
        main_song_title.setText(title);
//设置播放模式按钮
        //playMode 0:列表重复 1:随机 2:单曲重复 3:顺序
        if (MyApplication.getPlayMode() == MyConstant.list_repeat) {
            repeat_button.setImageResource(R.drawable.repeat);
            shuffle_button.setImageResource(R.drawable.shuffle_grey);
        } else if (MyApplication.getPlayMode() == MyConstant.random) {
            shuffle_button.setImageResource(R.drawable.shuffle);
            repeat_button.setImageResource(R.drawable.repeat_grey);
        } else if (MyApplication.getPlayMode() == MyConstant.one_repeat) {
            repeat_button.setImageResource(R.drawable.repeat_one);
            shuffle_button.setImageResource(R.drawable.shuffle_grey);
        } else {
            repeat_button.setImageResource(R.drawable.repeat_grey);
            shuffle_button.setImageResource(R.drawable.shuffle_grey);
        }
        //设置封面,自动封面获取颜色
        if (!nowMusic.getMusicLink().equals("")) {//网络
            final ImageView play_now_cover = (ImageView) findViewById(R.id.play_now_cover);
            Glide.with(this).load(nowMusic.getMusicAlbum()).listener(new RequestListener<String, GlideDrawable>() {
                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                    Bitmap resource = ((BitmapDrawable) getDrawable(R.drawable.default_album)).getBitmap();
                    Palette p = Palette.from(resource).generate();
                    animation_change_color(ColorUtil.getColor(p));
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    Palette p = Palette.from(ColorUtil.drawableToBitmap(resource)).generate();
                    animation_change_color(ColorUtil.getColor(p));
                    return false;
                }

            }).placeholder(R.drawable.default_album).into(play_now_cover);
        } else {
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri uri = ContentUris.withAppendedId(sArtworkUri, nowMusic.getMusicAlbumId());
            ImageView play_now_cover = (ImageView) findViewById(R.id.play_now_cover);
            Glide.with(this).load(uri).listener(new RequestListener<Uri, GlideDrawable>() {
                @Override
                public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                    Bitmap resource = ((BitmapDrawable) getDrawable(R.drawable.default_album)).getBitmap();
                    Palette p = Palette.from(resource).generate();
                    animation_change_color(ColorUtil.getColor(p));
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    Palette p = Palette.from(ColorUtil.drawableToBitmap(resource)).generate();
                    animation_change_color(ColorUtil.getColor(p));
                    return false;
                }
            }).placeholder(R.drawable.default_album).into(play_now_cover);
        }


        //设置歌曲名，歌手
        TextView play_now_song = (TextView) findViewById(R.id.play_now_song);
        TextView play_now_singer = (TextView) findViewById(R.id.play_now_singer);
        play_now_song.setText(title);
        play_now_singer.setText(artist);

        //设置播放按钮
        if (MyApplication.getState() == MyConstant.playing) {
            floatingActionButton.setImageResource(R.drawable.ic_pause_black_24dp);
            play_pause_button.setImageResource(R.drawable.ic_pause_black_24dp);
        } else if (MyApplication.getState() == MyConstant.pausing) {
            floatingActionButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            play_pause_button.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }


    }

    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void needsPermission() {
        sendPermissionGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @OnShowRationale({Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("权限概述")
                .setMessage("MediaPlayer如果缺少以下关键权限可能无法正常工作。\n\n储存权限：读取媒体、照片、文件权限用于获取歌曲信息并播放。")
                .setPositiveButton("重新授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton("仍不允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void permissionDenied() {
        new AlertDialog.Builder(this)
                .setTitle("权限概述")
                .setMessage("MediaPlayer如果缺少以下关键权限可能无法正常工作。\n\n储存权限：读取媒体、照片、文件权限用于获取歌曲信息并播放。")
                .setPositiveButton("重新授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivityPermissionsDispatcher.needsPermissionWithCheck(MainActivity.this);
                    }
                })
                .setNegativeButton("仍不允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();

    }

    private class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //更新UI
            if (intent.getIntExtra("UIChange", 0) == MyConstant.initialize) {
                FloatingActionButton random_play = (FloatingActionButton) findViewById(R.id.random_play);
                random_play.hide();
                SlidingUpPanelLayout mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
                mLayout.setPanelHeight((int) (60 * getResources().getDisplayMetrics().density + 0.5f));
            }

            if (intent.getIntExtra("UIChange", 0) == MyConstant.pauseAction) {
                ImageView play_pause_button = (ImageView) findViewById(R.id.play_pause_button);
                FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.play_or_pause);
                play_pause_button.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                floatingActionButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            }
            if (intent.getIntExtra("UIChange", 0) == MyConstant.playAction) {
                ImageView play_pause_button = (ImageView) findViewById(R.id.play_pause_button);
                FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.play_or_pause);
                play_pause_button.setImageResource(R.drawable.ic_pause_black_24dp);
                floatingActionButton.setImageResource(R.drawable.ic_pause_black_24dp);
            }
            if (intent.getIntExtra("UIChange", 0) == MyConstant.mediaChangeAction) {
                ChangeScrollingUpPanel(MyApplication.getPositionNow());
                Log.v("歌曲更换", "接收到");
                if (otherLyricView.getVisibility() == VISIBLE) {
                    changeVisibility();
                }
                TextView now_on_play_text = (TextView) findViewById(R.id.now_on_play_text);
                now_on_play_text.setText("正在播放");
                otherLyricView.loadLrc("");
            }
            if (intent.getIntExtra("onDestroy", 0) == 1) {
                finish();
            }
            if (intent.getIntExtra("viewPagerChange", -1) != -1) {
                isfromSc = true;
                viewPager.setCurrentItem(intent.getIntExtra("viewPagerChange", -1));
                isfromSc = false;
            }
        }

    }

    public void play_now_menu_button(View v) {
        if (MyApplication.getListlabel() != "netMusicList") {
            menu_util.popupMenu(this, v, MyApplication.getPositionNow(), MyApplication.getListlabel());
        } else {
            menu_util.popupNetMenu(this, v, MyApplication.getPositionNow());
        }

    }

    private void ensureServiceStarted() {
        if (MyApplication.getServiceState() == false) {
            Intent intent = new Intent(this, PlayService.class);
            intent.putExtra("ACTION", MyConstant.initialize);
            startService(intent);

        }
    }

    private class screenAdaptionTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            //检测网络模式设置
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            Boolean local_net_mode = sharedPref.getBoolean("local_net_mode", false);
            MyApplication.setLocal_net_mode(local_net_mode);
            return (int) (getResources().getDisplayMetrics().heightPixels * 0.6);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            ImageView play_now_cover = (ImageView) findViewById(R.id.play_now_cover);
            View lrcView = findViewById(R.id.other_lrc_view);
            RelativeLayout.LayoutParams lp_play_now_cover = (RelativeLayout.LayoutParams) play_now_cover.getLayoutParams();
            RelativeLayout.LayoutParams lp_lrcView = (RelativeLayout.LayoutParams) lrcView.getLayoutParams();
            lp_play_now_cover.height = (int) o;
            lp_lrcView.height = ((int) o / 3 * 2);
            play_now_cover.setLayoutParams(lp_play_now_cover);
            lrcView.setLayoutParams(lp_lrcView);

        }
    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            playService = ((PlayService.musicBinder) service).getService();
        }
    };

    private void updateSeekBar() {
        mTimer = new Timer();
        final TextView duration = (TextView) findViewById(R.id.duration);
        if (playService != null) {
            duration.setText(toTime(playService.getDuration()));
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (playService != null) {
                    if (MyApplication.getState() == MyConstant.playing) {
                        seekBar.setProgress(playService.getCurrentPosition());
                    }
                }
            }
        };
        mTimer.schedule(task, 0, 1000);
    }

    private void sleeper() {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        final int hourNow = c.get(java.util.Calendar.HOUR_OF_DAY);
        final int minuteNow = c.get(Calendar.MINUTE);
        new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourPicked, int minutePicked) {
                int duration = (hourPicked - hourNow) * 60 + minutePicked - minuteNow;
                if (hourPicked >= hourNow && duration > 0 && duration < 360) {
                    playService.deleteService(duration);
                    Snackbar.make(mLayout, "已经定时为" + duration + "分钟后关闭", Snackbar.LENGTH_LONG).setAction("确定", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        }
                    }).show();
                } else {
                    Snackbar.make(mLayout, "所选时间须为当天，且距当前时间6小时内", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        }
                    }).show();
                }
            }
        }, hourNow, minuteNow, true).show();
    }

    private void equalizer() {
        SharedPreferences bundle = MainActivity.this.getSharedPreferences("first_audioEffect", MainActivity.this.MODE_PRIVATE);
        if (bundle.getBoolean("isFirst", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("兼容性提醒");
            builder.setMessage("使用内置均衡器前，请确保未使用其他音效软件，否则可能因兼容性问题导致该均衡器无效。\n\n若您仍想使用内置均衡器，请先禁用手机内其他音效软件。");
            builder.setPositiveButton("我已了解，不再提醒", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(MainActivity.this, EqualizerActivity.class);
                    startActivity(intent);
                    SharedPreferences.Editor editor = getSharedPreferences("first_audioEffect", MODE_PRIVATE).edit();
                    editor.putBoolean("isFirst", false);
                    editor.apply();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(MainActivity.this, EqualizerActivity.class);
            startActivity(intent);
        }
    }

    private String toTime(int i) {
        int primary_second = i / 1000;
        int minute = primary_second / 60;
        int second = primary_second - minute * 60;
        return String.format("%2d", minute).replace(" ", "0") + ":" + String.format("%2d", second).replace(" ", "0");
    }

    private class getLyricTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            otherLyricView.setLabel("正在搜索歌词");
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                OkHttpClient client = new OkHttpClient();
                String input = strings[0];
                if (!strings[1].equals("未知歌手")) {
                    input = input + strings[1];
                }
                RequestBody requestBody = new FormBody.Builder().add("input", input).add("filter", "name").add("type", "netease").add("page", "1").build();
                Request request = new Request.Builder().url("https://music.2333.me").addHeader("X-Requested-With", "XMLHttpRequest").addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8").addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36").addHeader("Referer", "https://music.2333.me/?name=" + java.net.URLEncoder.encode(input, "utf-8") + "&type=netease").post(requestBody).build();
                Response response = client.newCall(request).execute();
                String res = response.body().string();
                JSONObject jsonObject = new JSONObject(res);
                if (jsonObject.getInt("code") == 200) {
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    String data = jsonArray.toString();
                    Gson gson = new Gson();
                    List<Music> musicList = gson.fromJson(data, new TypeToken<List<Music>>() {
                    }.getType());
                    for (int i = 0;i<musicList.size();i++){
                        if (musicList.get(i).getName().equals(strings[0])) {
                            return musicList.get(i).getLrc();
                        }
                    }
                    return "404";
                }
            } catch (Exception e) {
                if (e instanceof java.net.UnknownHostException) {
                    return "404";
                }else if (e instanceof java.net.SocketTimeoutException){
                    return "timeout";
                }
                e.printStackTrace();
            }
            return "404";
        }

        @Override
        protected void onPostExecute(String s) {
            switch (s) {
                case "404":
                    otherLyricView.setLabel("未搜索到匹配歌词");
                    if (!hasNetwork(MainActivity.this))
                        Snackbar.make(mLayout, "请检查您的网络", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                            }
                        }).show();
                    break;
                case "timeout":
                    Snackbar.make(mLayout, "服务器连接超时，请稍后再试", Snackbar.LENGTH_SHORT).setAction("确定", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        }
                    }).show();
                    break;
                default:
                    if (!s.equals("")) {
                        String lyric = praseLyric(s);
                        otherLyricView.loadLrc(lyric);
//                        savelyric(lyric);
                        otherLyricView.setOnPlayClickListener(new LrcView.OnPlayClickListener() {
                            @Override
                            public boolean onPlayClick(long time) {
                                fromLyric = true;
                                seekBar.setProgress((int) time);
                                return false;
                            }
                        });
                    } else {
                        otherLyricView.loadLrc("");
                        otherLyricView.setLabel("未搜索到匹配歌词");
                    }
                    break;
            }
            super.onPostExecute(s);
        }

    }

    private boolean hasNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.getState() == NetworkInfo.State.CONNECTED;
            }
        }
        return false;
    }

    private String praseLyric(String lyric) {
        try {
            String[] split = lyric.split("\n");
            if (split[split.length - 2].substring(9, 10).equals("]")) {
                //歌曲时间格式标准[04:28.46]
                return lyric;
            } else if (split[split.length - 2].substring(10, 11).equals("]")) {
                //歌曲时间格式不标准[04:28.660][04:28.660]
                lyric = "";
                Format f = new DecimalFormat("00");
                for (int i = 0; i < split.length; i++) {
                    if (!split[i].substring(1, 2).equals("0")) {
                        //有作者标签
                    } else {
                        //无作者标签
                        if (split[i].lastIndexOf("]") != 9) {
                            int allIndex = split[i].indexOf("]");
                            while (allIndex != -1) {
                                split[i] = split[i].substring(0, allIndex - 3) + String.valueOf(f.format(Integer.valueOf(split[i].substring(allIndex - 3, allIndex)) / 50 * 3)) + split[i].substring(allIndex);
                                allIndex = split[i].indexOf("]", allIndex + 1);
                            }
                        }
                        lyric = lyric + split[i] + "\n";
                    }
                }
                return lyric;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lyric;
    }

    private void changeVisibility() {
        View music_info_cardView = findViewById(R.id.music_info_cardView);
        View control_layout = findViewById(R.id.control_layout);
        View seekbar_layout = findViewById(R.id.seekbar_layout);
        View lrcView = findViewById(R.id.other_lrc_view);
        View gradient = findViewById(R.id.gradient);
        View gradient_bottom = findViewById(R.id.gradient_bottom);
        View gradient_top = findViewById(R.id.gradient_top);
        TransitionManager.beginDelayedTransition(transitionsContainer, set);
        if (music_info_cardView.getVisibility() == VISIBLE) {
            music_info_cardView.setVisibility(GONE);
            control_layout.setVisibility(GONE);
            seekbar_layout.setVisibility(GONE);
            lrcView.setVisibility(VISIBLE);
            gradient.setVisibility(VISIBLE);
            gradient_bottom.setVisibility(VISIBLE);
            gradient_top.setVisibility(VISIBLE);
//            mLayout.setDragView(R.id.play_now_cover);
        } else {
            music_info_cardView.setVisibility(VISIBLE);
            control_layout.setVisibility(VISIBLE);
            seekbar_layout.setVisibility(VISIBLE);
            lrcView.setVisibility(GONE);
            gradient.setVisibility(GONE);
            gradient_bottom.setVisibility(GONE);
            gradient_top.setVisibility(GONE);
//            mLayout.setDragView(R.id.activity_now_play);
        }
        visible = !visible;
    }

//    public void savelyric(String lyric) {
//        String name = MyApplication.getMusicListNow().get(MyApplication.getPositionNow()).getMusicTitle();
//        FileOutputStream out = null;
//        BufferedWriter writer = null;
//        try {
//            //设置文件名称，以及存储方式
//            out = openFileOutput(name, Context.MODE_PRIVATE);
//            //创建一个OutputStreamWriter对象，传入BufferedWriter的构造器中
//            writer = new BufferedWriter(new OutputStreamWriter(out));
//            //向文件中写入数据
//            writer.write(lyric);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
