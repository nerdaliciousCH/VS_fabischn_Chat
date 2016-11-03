package ch.ethz.inf.vs.a3.fabischn.udpclient;

import ch.ethz.inf.vs.a3.fabischn.chat.ChatActivity;

/**
 * Created by fabian on 03.11.16.
 */

public class DeregistrationResult {
    private boolean mDeregistered;
    private ChatActivity.BackActions mBackSource;
    private int mErrorCode;

    public DeregistrationResult(final boolean deregistered, final ChatActivity.BackActions backSource, final int errorCode){
        mDeregistered = deregistered;
        mBackSource = backSource;
        mErrorCode = errorCode;
    }

    public boolean getDeregisterStatus(){
        return mDeregistered;
    }

    public ChatActivity.BackActions getBackSource() {
        return mBackSource;
    }

    public int getErrorCode(){
        return mErrorCode;
    }
}
