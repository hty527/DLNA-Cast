package com.neulion.android.upnpcast.renderer.localservice;

import android.content.Context;

import com.neulion.android.upnpcast.renderer.localservice.IRendererInterface.IAVTransport;
import com.neulion.android.upnpcast.renderer.player.ICastControl;
import com.neulion.android.upnpcast.renderer.player.NLCastVideoPlayerActivity;
import com.neulion.android.upnpcast.renderer.utils.CastUtils;
import com.neulion.android.upnpcast.renderer.utils.ILogger;
import com.neulion.android.upnpcast.renderer.utils.ILogger.DefaultLoggerImpl;

import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.AVTransportErrorCode;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.model.DeviceCapabilities;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PlayMode;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.StorageMedium;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportSettings;

import java.net.URI;

public class AVTransportControlImp implements IAVTransport
{
    private final TransportAction[] TRANSPORT_ACTION_STOPPED = new TransportAction[]{TransportAction.Play};
    private final TransportAction[] TRANSPORT_ACTION_PLAYING = new TransportAction[]{TransportAction.Stop, TransportAction.Pause, TransportAction.Seek};
    private final TransportAction[] TRANSPORT_ACTION_PAUSE_PLAYBACK = new TransportAction[]{TransportAction.Stop, TransportAction.Pause, TransportAction.Seek, TransportAction.Play};

    private ILogger mLogger = new DefaultLoggerImpl(this);

    private final UnsignedIntegerFourBytes mInstanceId;

    private volatile TransportInfo currentTransportInfo = new TransportInfo();

    private PositionInfo currentPositionInfo = new PositionInfo();

    private MediaInfo currentMediaInfo = new MediaInfo();

    private TransportSettings mTransportSettings = new TransportSettings();

    private Context mApplicationContext;

    private ICastControl mControlListener;

    public AVTransportControlImp(Context context, UnsignedIntegerFourBytes instanceId, ICastControl listener)
    {
        mApplicationContext = context.getApplicationContext();

        mInstanceId = instanceId;

        mControlListener = listener;
    }

    public UnsignedIntegerFourBytes getInstanceId()
    {
        return mInstanceId;
    }

    public synchronized TransportAction[] getCurrentTransportActions()
    {
        if (currentTransportInfo != null)
        {
            switch (currentTransportInfo.getCurrentTransportState())
            {
                case STOPPED:
                    return TRANSPORT_ACTION_STOPPED;
                case PLAYING:
                    return TRANSPORT_ACTION_PLAYING;
                case PAUSED_PLAYBACK:
                    return TRANSPORT_ACTION_PAUSE_PLAYBACK;
            }
        }

        return null;
    }

    @Override
    public DeviceCapabilities getDeviceCapabilities()
    {
        return new DeviceCapabilities(new StorageMedium[]{StorageMedium.NETWORK});
    }

    @Override
    public MediaInfo getMediaInfo()
    {
        return currentMediaInfo;
    }

    @Override
    public PositionInfo getPositionInfo()
    {
        return currentPositionInfo;
    }

    @Override
    public TransportInfo getTransportInfo()
    {
        return currentTransportInfo;
    }

    @Override
    public TransportSettings getTransportSettings()
    {
        return mTransportSettings;
    }

    @Override
    public void setAVTransportURI(String currentURI, String currentURIMetaData) throws AVTransportException
    {
        mLogger.d(String.format("setAVTransportURI:[%s]", currentURI));
        mLogger.d(String.format("setAVTransportURI:[%s]", currentURIMetaData));

        // check currentURI
        try
        {
            new URI(currentURI);
        }
        catch (Exception ex)
        {
            throw new AVTransportException(ErrorCode.INVALID_ARGS, "CurrentURI can not be null or malformed");
        }

        //        if (currentURI.startsWith("http:"))
        //        {
        //            try
        //            {
        //                HttpFetch.validate(URIUtil.toURL(uri));
        //            }
        //            catch (Exception ex)
        //            {
        //                throw new AVTransportException(AVTransportErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        //            }
        //        }
        //        else if (!currentURI.startsWith("file:"))
        //        {
        //            throw new AVTransportException(ErrorCode.INVALID_ARGS, "Only HTTP and file: resource identifiers are supported");
        //        }

        currentMediaInfo = new MediaInfo(currentURI, currentURIMetaData, getInstanceId(), "", StorageMedium.NETWORK);

        currentPositionInfo = new PositionInfo(1, currentURIMetaData, currentURI);

        if (mApplicationContext != null)
        {
            NLCastVideoPlayerActivity.startActivity(mApplicationContext, currentURI, currentURIMetaData);
        }
    }

    @Override
    public void setNextAVTransportURI(String nextURI, String nextURIMetaData)
    {
        mLogger.d(String.format("setNextAVTransportURI:[%s]", nextURI));
        mLogger.d(String.format("setNextAVTransportURI:[%s]", nextURIMetaData));
    }

    @Override
    public void play(String speed)
    {
        mLogger.d("play: " + speed);

        mControlListener.play();
    }

    public void pause()
    {
        mLogger.d("pause");

        mControlListener.pause();
    }

    @Override
    public void seek(String unit, String target) throws AVTransportException
    {
        mLogger.d(String.format("seek [%s][%s]", unit, target));

        SeekMode seekMode = SeekMode.valueOrExceptionOf(unit);

        if (!seekMode.equals(SeekMode.REL_TIME))
        {
            throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek mode: " + unit);
        }

        long position = CastUtils.getIntTime(target);

        mControlListener.seek(position);
    }

    synchronized public void stop()
    {
        mLogger.d("stop");

        mControlListener.stop();
    }

    @Override
    public void previous()
    {
        mLogger.d("previous");
    }

    @Override
    public void next()
    {
        mLogger.d("next");
    }

    @Override
    public void record()
    {
        mLogger.d("record");
    }

    @Override
    public void setPlayMode(String newPlayMode)
    {
        mLogger.d(String.format("setPlayMode:[%s]", newPlayMode));

        if (!newPlayMode.equalsIgnoreCase(PlayMode.NORMAL.name()))
        {
            throw new IllegalArgumentException("Only accept 'NORMAL' playMode!!!");
        }
    }

    @Override
    public void setRecordQualityMode(String newRecordQualityMode)
    {
        mLogger.d(String.format("setRecordQualityMode:[%s]", newRecordQualityMode));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // - Update
    // ----------------------------------------------------------------------------------------------------------------
    @Override
    public void setCurrentPosition(long position)
    {
        currentPositionInfo.setRelTime(CastUtils.getStringTime(position));
    }

    @Override
    public void setDuration(long duration)
    {
        currentPositionInfo.setTrackDuration(CastUtils.getStringTime(duration));
    }
}