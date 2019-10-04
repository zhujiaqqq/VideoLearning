package com.example.videorecorder;

interface OnRecordListener {
    /**
     * 录制完成
     */
    void onRecordFinish();

    /**
     * 录制进度
     *
     * @param progress 进度
     */
    void onRecordProgress(int progress);
}
