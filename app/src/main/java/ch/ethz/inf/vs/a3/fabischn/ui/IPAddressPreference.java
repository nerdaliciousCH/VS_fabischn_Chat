package ch.ethz.inf.vs.a3.fabischn.ui;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;

import org.w3c.dom.Attr;

/**
 * Created by fabian on 28.10.16.
 *
 * fabischn: add TextWatcher to check if correct IP format
 * http://stackoverflow.com/questions/8661915/what-androidinputtype-should-i-use-for-entering-an-ip-address
 * http://stackoverflow.com/questions/3882918/ip-address-textbox-in-android
 */
public class IPAddressPreference extends EditTextPreference {

    public IPAddressPreference(Context context , AttributeSet attributeSet) {
        super(context, attributeSet);

        getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        getEditText().setFilters(new InputFilter[] { new InputFilter(){
            @Override
            public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);

                    // TODO use Patterns.IP_ADDRESS.matcher(input).macthes() == true
                    if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    }
                    else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        }
        });

        getEditText().addTextChangedListener(new TextWatcher(){
            boolean deleting = false;
            int lastCount = 0;

            @Override
            public void afterTextChanged(Editable s) {
                if (!deleting) {
                    String working = s.toString();
                    String[] split = working.split("\\.");
                    String string = split[split.length - 1];
                    if (string.length() == 3 || string.equalsIgnoreCase("0")
                            || (string.length() == 2 && Character.getNumericValue(string.charAt(0)) > 1)) {
                        s.append('.');
                        return;
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (lastCount < count) {
                    deleting = false;
                }
                else {
                    deleting = true;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing happens here
            }
        });
    }
}
