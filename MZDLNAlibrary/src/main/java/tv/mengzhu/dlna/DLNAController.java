package tv.mengzhu.dlna;

import android.content.Context;
import android.widget.Toast;


import org.fourthline.cling.support.model.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tv.mengzhu.dlna.callback.ControlCallback;
import tv.mengzhu.dlna.entity.RemoteItem;
import tv.mengzhu.dlna.manager.ClingManager;
import tv.mengzhu.dlna.manager.ControlManager;
import tv.mengzhu.dlna.manager.DeviceManager;

/**
 * Created by DELL on 2019/3/5.
 */
public class DLNAController {

    private Item localItem;
    private RemoteItem remoteItem;
    private Context mContext;
    private DLNAStateListener mListener;
    private Map<String,DLNAStateListener> mListenerMap;
    private static DLNAController mPresenter;


    public static final int PLAYER_DLNA=0;
    public static final int STOP_DLNA=1;
    public static final int PAUSE_DLNA=3;

    public enum DLNAState{
        PAUSE_STATE,
        PLAY_STATE,
        STOP_STATE
    }
    public enum DLNAError{
        PLAY_ERROR,
        PAUSE_ERROR,
        STOP_ERROR
    }

    public interface DLNAStateListener{
        public void onSuccess(DLNAState state);
        public void onError(DLNAError error);
    }

    public static DLNAController getInstance(Context context){
        if(mPresenter==null){
            mPresenter=new DLNAController();
            mPresenter.initPresenter(context);
        }
        return mPresenter;
    }

    public void registerListener(String key,DLNAStateListener listener){
        if(mListenerMap!=null&&!mListenerMap.containsKey(key)) {
            mListenerMap.put(key, listener);
        }
    }

    public void unRegisterListener(String key){
        if(mListenerMap!=null&&mListenerMap.containsKey(key)) {
            mListenerMap.remove(key);
        }
    }

    public void onExecuteSuccessListener(DLNAState state){
        Iterator iter = mListenerMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            DLNAStateListener val = (DLNAStateListener) entry.getValue();
            val.onSuccess(state);

        }
    }
    public void onExecuteErrorListener(DLNAError error){
        Iterator iter = mListenerMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            DLNAStateListener val = (DLNAStateListener) entry.getValue();
            val.onError(error);

        }
    }


    public void initPresenter(Context context) {
        mContext=context;
        VApplication.init(context);
        ClingManager.getInstance().startClingService();
        mListenerMap=new HashMap<>();
    }

    public void onExecute(Object... obj) {
        int index= (int) obj[0];
        switch (index){
            case PLAYER_DLNA:
                play();
                break;
            case STOP_DLNA:
                stop();
                break;
            case PAUSE_DLNA:
                pauseCast();
                break;
            default:
                break;
        }
    }
    public void onDestroy(){
        ControlManager.getInstance().unInitScreenCastCallback();
        ClingManager.getInstance().destroy();
        DeviceManager.getInstance().destroy();
    }

    public void registerListener(Object listener) {
        mListener= (DLNAStateListener) listener;
    }

    /**
     * 播放开关
     */
    private void play() {
        if (ControlManager.getInstance().getState() == ControlManager.CastState.STOPED) {
            localItem = ClingManager.getInstance().getLocalItem();
            remoteItem = ClingManager.getInstance().getRemoteItem();
            if (localItem != null) {
                newPlayCastLocalContent();
            } else {
                newPlayCastRemoteContent();
            }
        } else if (ControlManager.getInstance().getState() == ControlManager.CastState.PAUSED) {
            playCast();
        } else if (ControlManager.getInstance().getState() == ControlManager.CastState.PLAYING) {
            pauseCast();
        } else {
            Toast.makeText(mContext, "正在连接设备，稍后操作", Toast.LENGTH_SHORT).show();
        }
    }

    private void stop() {
        ControlManager.getInstance().unInitScreenCastCallback();
        stopCast();
    }

    private void pauseCast() {
        ControlManager.getInstance().pauseCast(new ControlCallback() {
            @Override
            public void onSuccess() {
                ControlManager.getInstance().setState(ControlManager.CastState.PAUSED);
                if(mListener!=null) {
                    mListener.onSuccess(DLNAState.PAUSE_STATE);
                }
                onExecuteSuccessListener(DLNAState.PAUSE_STATE);
            }

            @Override
            public void onError(int code, String msg) {
                if(mListener!=null) {
                    mListener.onError(DLNAError.PAUSE_ERROR);
                }
                onExecuteErrorListener(DLNAError.PAUSE_ERROR);
            }
        });
    }

    private void newPlayCastLocalContent() {
        ControlManager.getInstance().setState(ControlManager.CastState.TRANSITIONING);
        ControlManager.getInstance().newPlayCast(ClingManager.getInstance().getLocalItem(), new ControlCallback() {
            @Override
            public void onSuccess() {
                ControlManager.getInstance().setState(ControlManager.CastState.PLAYING);
                ControlManager.getInstance().initScreenCastCallback();
                if(mListener!=null) {
                    mListener.onSuccess(DLNAState.PLAY_STATE);
                }
                onExecuteSuccessListener(DLNAState.PLAY_STATE);
            }

            @Override
            public void onError(int code, String msg) {
                ControlManager.getInstance().setState(ControlManager.CastState.STOPED);
                if(mListener!=null) {
                    mListener.onError(DLNAError.PAUSE_ERROR);
                }
                onExecuteErrorListener(DLNAError.PAUSE_ERROR);
            }
        });
    }

    private void newPlayCastRemoteContent() {
        ControlManager.getInstance().setState(ControlManager.CastState.TRANSITIONING);
        ControlManager.getInstance().newPlayCast(remoteItem, new ControlCallback() {
            @Override
            public void onSuccess() {
                ControlManager.getInstance().setState(ControlManager.CastState.PLAYING);
                ControlManager.getInstance().initScreenCastCallback();
                if(mListener!=null) {
                    mListener.onSuccess(DLNAState.PLAY_STATE);
                }
                onExecuteSuccessListener(DLNAState.PLAY_STATE);
            }

            @Override
            public void onError(int code, String msg) {
                ControlManager.getInstance().setState(ControlManager.CastState.STOPED);
                if(mListener!=null) {
                    mListener.onError(DLNAError.PLAY_ERROR);
                }
                onExecuteErrorListener(DLNAError.PLAY_ERROR);
            }
        });
    }

    private void playCast() {
        ControlManager.getInstance().playCast(new ControlCallback() {
            @Override
            public void onSuccess() {
                ControlManager.getInstance().setState(ControlManager.CastState.PLAYING);
                if(mListener!=null){
                    mListener.onSuccess(DLNAState.PLAY_STATE);
                }
                onExecuteSuccessListener(DLNAState.PLAY_STATE);
            }

            @Override
            public void onError(int code, String msg) {
                if(mListener!=null){
                    mListener.onError(DLNAError.PLAY_ERROR);
                }
                onExecuteErrorListener(DLNAError.PLAY_ERROR);
            }
        });
    }


    private void stopCast() {
        ControlManager.getInstance().stopCast(new ControlCallback() {
            @Override
            public void onSuccess() {
                ControlManager.getInstance().setState(ControlManager.CastState.STOPED);
                if(mListener!=null){
                    mListener.onSuccess(DLNAState.STOP_STATE);
                }
                onExecuteSuccessListener(DLNAState.STOP_STATE);
            }

            @Override
            public void onError(int code, String msg) {
                if(mListener!=null){
                    mListener.onError(DLNAError.STOP_ERROR);
                }
                onExecuteErrorListener(DLNAError.STOP_ERROR);
            }
        });
    }

}
