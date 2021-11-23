package com.vdotok.many2many.base


import com.vdotok.network.models.GroupModel
import com.vdotok.streaming.models.CallParams
import org.webrtc.VideoTrack

/**
 * Interface that are to be implemented in order provide callbacks to fragments
 * */
interface FragmentCallback {
    fun onIncomingCall(model: CallParams)
    fun onStartCalling()
    fun outGoingCall(toPeer : GroupModel)
    //for video steam
    fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String)
    //for audio steam
    fun onRemoteStreamReceived(refId: String, sessionID: String) {}
    fun onCameraStreamReceived(stream: VideoTrack)
    fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String)
    fun onCallMissed()
    fun onCallerAlreadyBusy() {}
    fun onCallRejected(reason: String)
    fun onCallEnd() {}
    fun onConnectionSuccess() {}
    fun onConnectionFail() {}
    fun onParticipantLeftCall(refId: String?)
    fun noAnsFromTarget() {}
    fun onAudioVideoStateChanged(audioState: Int, videoState: Int) {}
    fun endOngoingCall(sessionId: String) {}
    fun onCallTimeout() {}

}