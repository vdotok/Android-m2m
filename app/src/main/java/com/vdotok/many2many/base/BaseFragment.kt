package com.vdotok.many2many.base

import androidx.fragment.app.Fragment
import com.vdotok.network.models.GroupModel
import com.vdotok.streaming.models.CallParams
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 5/26/21 At 3:21 PM in 2021
 */
open class BaseFragment: Fragment(), FragmentCallback {

    override fun onStart() {
        BaseActivity.mListener = this
        super.onStart()
    }
    override fun onIncomingCall(model: CallParams) {}
    override fun onStartCalling() {}
    override fun outGoingCall(toPeer: GroupModel) {}
    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {}

    override fun onCameraStreamReceived(stream: VideoTrack) {}
    override fun onCameraAudioOff(audioState: Int, videoState: Int, refId: String) {}

    override fun onCallMissed() {}
    override fun onCallRejected(reason: String) {}

    override fun onParticipantLeftCall(refId: String?) {}
    override fun endOngoingCall(sessionId: String) {}
    override fun onAudioVideoStateChanged(audioState: Int, videoState: Int) {}

}