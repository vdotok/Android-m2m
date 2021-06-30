package com.norgic.vdotokcall_mtm.interfaces

import com.norgic.vdotokcall_mtm.models.AcceptCallModel
import com.norgic.vdotokcall_mtm.models.GroupModel
import org.webrtc.VideoTrack

/**
 * Interface that are to be implemented in order provide callbacks to fragments
 * */
interface FragmentRefreshListener {
    fun onIncomingCall(model: AcceptCallModel)
    fun onStartCalling()
    fun outGoingCall(toPeer : GroupModel)
    //for video steam
    fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String)
    //for audio steam
    fun onRemoteStreamReceived(refId: String, sessionID: String) {}
    fun onCameraStreamReceived(stream: VideoTrack)
    fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String)
    fun onCallMissed()
    fun onCallRejected(reason: String)
    fun onCallEnd() {}
    fun onParticipantLeftCall(refId: String?)
}