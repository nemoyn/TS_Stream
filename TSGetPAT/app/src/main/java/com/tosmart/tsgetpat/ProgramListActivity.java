package com.tosmart.tsgetpat;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.tosmart.tsgetpat.adapter.ProgramListAdapter;
import com.tosmart.tsgetpat.beans.tables.Pat;
import com.tosmart.tsgetpat.beans.tables.PatProgram;
import com.tosmart.tsgetpat.threads.GetPidPacketThread;
import com.tosmart.tsgetpat.utils.PacketManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ProgramListActivity
 *
 * @author ggz
 * @date 2018/3/27
 */

public class ProgramListActivity extends AppCompatActivity {
    private static final String TAG = "ProgramListActivity";
    private static final int PAT_PID = 0x0000;
    private static final int PAT_TABLE_ID = 0x00;
    public static final int REFRESH_UI_PROGRAM_LIST = 0;
    private static final String HISTORY_FOLDER_PATH = Environment.getExternalStorageDirectory()
            .getPath() + "/ts_history/";


    private SmartRefreshLayout mRefreshLayout;

    private String mTSFolderPath;
    private String mFileName;
    private String mFilePath;

    private PacketManager mPacketManager;
    private List<PatProgram> mPatProgramList = new ArrayList<>();
    private ProgramListAdapter mAdapter;

    private GetPidPacketThread mGetPidPacketThread;

    Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_UI_PROGRAM_LIST:
                    mRefreshLayout.autoRefresh();
                    break;

                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.program_list_activity);

        // 获取输入文件的路径和名字
        mTSFolderPath = getIntent().getStringExtra(FileListActivity.KEY_FOLDER_PATH);
        mFileName = getIntent().getStringExtra(FileListActivity.KEY_FILE_NAME);
        mFilePath = mTSFolderPath + mFileName;

        initView();

        initData();
    }

    private void initView() {
        ImageView backIv = findViewById(R.id.iv_back);
        backIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        RecyclerView programListRv = findViewById(R.id.rv_program_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        programListRv.setLayoutManager(layoutManager);
        mAdapter = new ProgramListAdapter(this, mPatProgramList);
        programListRv.setAdapter(mAdapter);


        mRefreshLayout = findViewById(R.id.refresh_layout_refresh_program_list);
        mRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                Pat pat = mPacketManager.getPat();
                if (pat != null) {
                    mPatProgramList.clear();
                    List<PatProgram> patProgramList = pat.getPatProgramList();
                    for (PatProgram patProgram : patProgramList) {
                        mPatProgramList.add(patProgram);
                    }
                    mAdapter.notifyDataSetChanged();
                }
                mRefreshLayout.finishRefresh();
            }
        });

        mRefreshLayout.autoRefresh();
    }

    private void initData() {
        if (mFilePath != null) {
            String inputFilePath = mFilePath;

            boolean isFindHistoryFile = false;
            String historyFilePath = findHistoryFile(mFileName + "_pat");
            if (historyFilePath != null) {
                isFindHistoryFile = true;
                inputFilePath = historyFilePath;
            }

            // 解 PAT
            mPacketManager = new PacketManager(
                    inputFilePath,
                    PAT_PID,
                    PAT_TABLE_ID,
                    mUIHandler);
            // 非必须：文件输出路径
            if (!isFindHistoryFile) {
                mPacketManager.setOutputFilePath(HISTORY_FOLDER_PATH + mFileName + "_pat");
            }

            Log.d(TAG, " ---- 开启线程");
            mGetPidPacketThread = new GetPidPacketThread(
                    mPacketManager,
                    mUIHandler);
            mGetPidPacketThread.start();
        }
    }

    private String findHistoryFile(String searchName) {
        String path;
        File file = new File(HISTORY_FOLDER_PATH);
        if (!file.exists()) {
            file.mkdir();
        }
        String[] fileList = file.list();
        if (fileList != null) {
            for (String str : fileList) {
                if (str.equals(searchName)) {
                    path = HISTORY_FOLDER_PATH + searchName;
                    return path;
                }
            }
        }
        return null;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGetPidPacketThread.over();

    }
}
