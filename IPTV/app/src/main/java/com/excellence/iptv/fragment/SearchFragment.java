package com.excellence.iptv.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.excellence.iptv.MainActivity;
import com.excellence.iptv.PlayerActivity;
import com.excellence.iptv.R;
import com.excellence.iptv.adapter.ProgramListAdapter;
import com.excellence.iptv.bean.Program;
import com.excellence.iptv.broadcast.MyActoin;
import com.excellence.iptv.view.FlowLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.excellence.iptv.fragment.LiveFragment.KEY_PROGRAM_NUM;
import static com.excellence.iptv.fragment.LiveFragment.KEY_TS;

/**
 * SearchFragment
 *
 * @author ggz
 * @date 2018/4/3
 */

public class SearchFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "SearchFragment";
    private static final String KEY_SP_HISTORY_TAG = "HistoryTag";

    private View mView;
    private MainActivity mMainActivity;


    private LocalBroadcastManager mLocalBroadcastManager;
    private MyLocalReceiver mLocalReceiver;

    private List<Program> mProgramList = new ArrayList<>();

    private MaterialEditText mEditText;
    private RecyclerView mRecyclerView;
    private List<Program> mSearchResultList = new ArrayList<>();
    private ProgramListAdapter mSearchResultListAdapter;

    private LinearLayout mSearchHistoryLl;
    private FlowLayout mFlowLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.search_fragment, container, false);
        mMainActivity = (MainActivity) getActivity();

        // 从 Activity 获取节目列表数据
        mProgramList = mMainActivity.getProgramList();

        // 注册本地广播监听器
        initLocalBroadcast();

        // 初始化控件和数据
        initView(mView);
        initHistoryTagData();

        // 初始化 MaterialEditText
        initMaterialEditText(mView);

        // 初始化 Search Result 列表
        initSearchResultRv(mView);

        return mView;
    }

    private void initLocalBroadcast() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mMainActivity);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyActoin.SEND_SEARCH_FRAGMENT_LOCAL_ACTION);
        mLocalReceiver = new MyLocalReceiver();
        mLocalBroadcastManager.registerReceiver(mLocalReceiver, intentFilter);
    }

    private void initView(View v) {
        ImageView searchIv = v.findViewById(R.id.iv_search);
        searchIv.setOnClickListener(this);

        Button cancelBtn = v.findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(this);
        AnimationSet animationSet = (AnimationSet) AnimationUtils
                .loadAnimation(mMainActivity, R.anim.view_scale_in);
        cancelBtn.startAnimation(animationSet);

        mSearchHistoryLl = v.findViewById(R.id.ll_search_history);
        AnimationSet animationSet1 = (AnimationSet) AnimationUtils
                .loadAnimation(mMainActivity, R.anim.view_translate_in);
        mSearchHistoryLl.startAnimation(animationSet1);

        mFlowLayout = v.findViewById(R.id.flow_layout_history_tag);
        mFlowLayout.setOnTagClickListener(new FlowLayout.OnTagClickListener() {
            @Override
            public void onClick(String text) {
                mEditText.setText(text);
            }
        });

        ImageView clearTagIv = v.findViewById(R.id.iv_clear_tag);
        clearTagIv.setOnClickListener(this);
    }

    private void initHistoryTagData() {
        // 读取 SharedPreferences 里面的 History Tag
        SharedPreferences sp = mMainActivity.getPreferences(Context.MODE_PRIVATE);
        String json = sp.getString(KEY_SP_HISTORY_TAG, null);
        if (json != null) {
            List<String> list = new Gson().fromJson(json, new TypeToken<List<String>>() {
            }.getType());
            mFlowLayout.setListData(list);
        }
    }

    private void initMaterialEditText(View v) {
        mEditText = v.findViewById(R.id.et_search);
        mEditText.requestFocus();
        showKeyboard(true);

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // 添加历史标签
                    String content = mEditText.getText().toString();
                    content = formatString(content);
                    if (!TextUtils.isEmpty(content)) {
                        mFlowLayout.addTag(content);
                    }
                    showKeyboard(false);
                }
                return false;
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String content = s.toString();
                mRecyclerView.setVisibility(View.VISIBLE);

                if (!TextUtils.isEmpty(content)) {
                    mSearchResultList.clear();
                    searchItem(content);
                    mSearchResultListAdapter.notifyDataSetChanged();
                } else {
                    // 隐藏搜索结果列表
                    mRecyclerView.setVisibility(View.GONE);
                    AnimationSet animationSet = (AnimationSet) AnimationUtils
                            .loadAnimation(mMainActivity, R.anim.view_translate_in);
                    mSearchHistoryLl.startAnimation(animationSet);
                }

            }
        });
    }

    private void searchItem(String str) {
        String searchStr = formatString(str);

        for (int i = 0; i < mProgramList.size(); i++) {
            String programName = mProgramList.get(i).getProgramName();
            programName = formatString(programName);
            int index = programName.indexOf(searchStr);

            String programNum = String.valueOf(mProgramList.get(i).getProgramNumber());
            int index2 = programNum.indexOf(searchStr);
            if (index != -1 || index2 != -1) {
                mSearchResultList.add(mProgramList.get(i));
            }
        }
    }

    private String formatString(String inputStr) {
        String str = inputStr.replaceAll(" ", "");
        str = str.toLowerCase();
        return str;
    }

    private void initSearchResultRv(View v) {
        mRecyclerView = v.findViewById(R.id.rv_search_result);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mMainActivity);
        mRecyclerView.setLayoutManager(layoutManager);
        mSearchResultListAdapter = new ProgramListAdapter(mMainActivity, mSearchResultList);
        mRecyclerView.setAdapter(mSearchResultListAdapter);

        mSearchResultListAdapter.setOnItemClickListener(new ProgramListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String programName = mSearchResultList.get(position).getProgramName();
                int programNum = mProgramList.get(position).getProgramNumber();

                Intent intent = new Intent(mMainActivity, PlayerActivity.class);
                intent.putExtra(KEY_TS, mMainActivity.getTs());
                intent.putExtra(KEY_PROGRAM_NUM, programNum);
                mMainActivity.startActivity(intent);

                // 添加历史标签
                mFlowLayout.addTag(programName);
                // 清空搜索框
                mEditText.setText("");
                // 隐藏键盘
                showKeyboard(false);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_search:
                // 添加历史标签
                String content = mEditText.getText().toString();
                content = formatString(content);
                if (!TextUtils.isEmpty(content)) {
                    mFlowLayout.addTag(content);
                }
                showKeyboard(false);
                break;

            case R.id.btn_cancel:
                mFlowLayout.editMode(false);
                Intent intent = new Intent(MyActoin.SEARCH_FRAGMENT_BACK_LOCAL_ACTION);
                mLocalBroadcastManager.sendBroadcast(intent);
                break;

            case R.id.iv_clear_tag:
                mFlowLayout.cleanTag();
                mFlowLayout.editMode(false);
                break;

            default:
                break;
        }
    }

    private void showKeyboard(boolean isShow) {
        InputMethodManager imm = (InputMethodManager)
                mMainActivity.getSystemService(INPUT_METHOD_SERVICE);
        if (isShow) {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        } else {
            imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);
        }

    }


    /**
     * 监听本地广播
     */
    private class MyLocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MyActoin.SEND_SEARCH_FRAGMENT_LOCAL_ACTION)) {
                if (mFlowLayout.getIsEditMode()) {
                    mFlowLayout.editMode(false);
                } else {

                    if (!TextUtils.isEmpty(mEditText.getText())) {
                        mEditText.setText("");
                    } else {
                        Intent intent0 = new Intent(MyActoin.SEARCH_FRAGMENT_BACK_LOCAL_ACTION);
                        mLocalBroadcastManager.sendBroadcast(intent0);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mLocalBroadcastManager.unregisterReceiver(mLocalReceiver);

        // History Tag 存入 SharedPreferences
        List<String> list = mFlowLayout.getHistoryTagList();
        String json = new Gson().toJson(list);
        SharedPreferences.Editor editor = mMainActivity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(KEY_SP_HISTORY_TAG, json);
        editor.apply();


    }
}
