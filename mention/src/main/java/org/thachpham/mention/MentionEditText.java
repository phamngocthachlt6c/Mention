package org.thachpham.mention;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import com.thachpham.mention.R;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

public class MentionEditText extends AppCompatEditText {
    private final int STATE_NONE = 0;
    private final int STATE_SHOW_MENTION_LIST = 1;
    private EditText mEditTextMessage;
    private String s = "";
    private String denotationCharacter = "@";
    private String MENTION_TAIL = " ";
    // This flag for prevent call methods in onBeforeTextChanged after set the text before
    private boolean preventActionAfterSetText = false;
    private boolean preventCheckOpenMentionList = false;
    private boolean reformatMessage = false;
    private boolean deleteMentionByAddingCharacter = false;
    private Mention mentionWillBeDeleted = null;
    private int cursorIndexForReformatMessage;
    private List<Mention> mMentions;
    private int mState = STATE_NONE;
    private int cursorBeginTextSearch;

    private ActionListener mActionListener;

    private Context mContext;

    public MentionEditText(Context context) {
        super(context);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        mMentions = new ArrayList<>();

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence charSequence, int start, int before, int count) {
                if (preventCheckOpenMentionList) {
                    preventCheckOpenMentionList = false;
                    return;
                }
                String message = mEditTextMessage.getText().toString();
                switch (mState) {
                    case STATE_NONE:
                        if (message.length() == 0) {
                            hideMentionList();
                        } else if (message.length() == 1) {
                            if (message.equals(denotationCharacter)) {
                                showMentionList(1);
                            } else {
                                hideMentionList();
                            }
                        } else if (start != 0 && start < message.length()) {
                            //start = 0 in first time callback called after move cursor to the new index => start need to be difference 0 => below param in substring method != -1
                            String lastWord = message.substring(start - 1, start + 1);
                            if (lastWord.equals(String.format(" %s", denotationCharacter)) || lastWord.equals(String.format("\n%s", denotationCharacter))) {
                                showMentionList(start + 1);
                            } else {
                                hideMentionList();
                            }
                        } else if(start == 0) {
                            String lastWord = message.substring(0, 1);
                            if (lastWord.equals(String.format("%s", denotationCharacter))) {
                                showMentionList(1);
                            } else {
                                hideMentionList();
                            }
                        }
                        break;
                    case STATE_SHOW_MENTION_LIST:
                        int cursorIndex = mEditTextMessage.getSelectionStart();
                        if (cursorIndex == cursorBeginTextSearch - 1) {
                            hideMentionList();
                        } else if (cursorIndex >= cursorBeginTextSearch) {
                            if (mActionListener != null) {
                                mActionListener.onSearchListMention(message.substring(cursorBeginTextSearch, cursorIndex));
                            }
                        }
                        break;
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {
            }
        });

        /**
         * Loop set text and change text in this listener very difficult, please notice FLAGS
         * Flags: preventActionAfterSetText
         */
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

//                Log.d(TAG, "aaaaa: start = " + start + ", count = " + count + ", after = " + after + ", s = " + s);
                if (preventActionAfterSetText) {
                    preventActionAfterSetText = false;
                    return;
                }
                Mention mention = isThereMentionHere(mEditTextMessage.getSelectionStart());
                if (after > count) {
                    if (mention != null) {
                        mentionWillBeDeleted = mention;
                        deleteMentionByAddingCharacter = true;
                    } else {
                        changeListMentionInfoAfterAddCharacters(mEditTextMessage.getSelectionStart(), after - count);
                    }
                } else if (after < count) {
                    if (mention != null) {
                        deleteMention(mention, false);
                    } else {
                        changeListMentionInfoAfterDeleteCharacters(mEditTextMessage.getSelectionStart(), count - after);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                Log.d(TAG, "aaaaa: start = " + start + ", count = " + count + ", before = " + before + ", s = " + s);
//                Log.d("aaaa", "onTextChanged: cursor start = " + mEditTextMessage.getSelectionStart() + ", cursor end = " + mEditTextMessage.getSelectionEnd());
                if (deleteMentionByAddingCharacter) {
                    deleteMentionByAddingCharacter = false;
                    if (mentionWillBeDeleted != null) {
                        String characterAdded = "";
                        try {
                            characterAdded = String.valueOf(s.charAt(start));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        deleteMentionByAddingCharacter(mentionWillBeDeleted, characterAdded);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (reformatMessage) {
                    preventActionAfterSetText = true;
                    reformatMessage = false;
                    setTextMessage(mEditTextMessage.getText().toString());
                    try {
                        mEditTextMessage.setSelection(cursorIndexForReformatMessage);
                    } catch (Exception e) {
                        mEditTextMessage.setSelection(mEditTextMessage.getText().toString().length());
                    }
                }
            }
        });
    }

    /**
     * Method add denotation "@name" to the message
     */
    public void addMentionUser(MentionInput mentionInput) {
        hideMentionList();
        preventActionAfterSetText = true;
        preventCheckOpenMentionList = true;
        String currentMessage = mEditTextMessage.getText().toString();
        int cursorPosition = mEditTextMessage.getSelectionStart();
        Mention mentionHere = isThereMentionHere(cursorPosition);
        if (mentionHere != null) {
            mMentions.remove(mentionHere);
        }

        // add new mention to manage
        Mention mention = new Mention();
        mention.setMentionText(mentionInput.getTitle());

        /**
         * Warning: Please noticing and keeping order of calling methods below
         * When every time you change these methods called order, that make a large change, so be careful
         * Sometime you see I called duplicated method in two conditions, but that is not wrong, please keep it!
         */
        // Has denotation "@" at the beginning
        mention.setStartInMessage(cursorBeginTextSearch - 1);
        mention.setEndInMessage(cursorBeginTextSearch + mention.getMentionText().length() - 1);
        // TODO: mentions update wrong because it was updated when typing some text that
        // plus 1 because mention text has not "@"
        changeListMentionInfoAfterAddCharacters(cursorBeginTextSearch, mention.getMentionText().length() - (cursorPosition - cursorBeginTextSearch) + MENTION_TAIL.length());
        mMentions.add(mention);
        setTextMessage(String.format("%s%s%s", currentMessage.substring(0, cursorBeginTextSearch), mention.getMentionText() + MENTION_TAIL, currentMessage.substring(cursorPosition)));
        mEditTextMessage.setSelection(cursorBeginTextSearch + mention.getMentionText().length() + MENTION_TAIL.length());
    }

    public void addDenotation() {
        preventActionAfterSetText = true;
        preventCheckOpenMentionList = true;
        // TODO: case replace a string by a character
        int cursorIndex = mEditTextMessage.getSelectionStart();
        cursorBeginTextSearch = cursorIndex + 1;
        mState = STATE_SHOW_MENTION_LIST;
        Mention mentionHere = isThereMentionHere(cursorIndex);
        if (mentionHere != null) {
            mMentions.remove(mentionHere);
        }
        int cursorEndIndex = mEditTextMessage.getSelectionEnd();
        int lengthChange = cursorEndIndex - cursorIndex + 1;
        String message = mEditTextMessage.getText().toString();
        message = message.substring(0, cursorIndex) + denotationCharacter + message.substring(cursorEndIndex);
        changeListMentionInfoAfterDeleteCharacters(cursorIndex, lengthChange - 1);
        changeListMentionInfoAfterAddCharacters(cursorIndex, 1);
        setTextMessage(message);
        mEditTextMessage.setSelection(cursorIndex + 1);
    }

    private void deleteMention(Mention mention, boolean deleteAllTextInMention) {
        preventActionAfterSetText = true;
        mMentions.remove(mention);
        String message = mEditTextMessage.getText().toString();
        if (deleteAllTextInMention) {
            changeListMentionInfoAfterDeleteCharacters(mEditTextMessage.getSelectionStart(), mention.getMentionText().length() + 1);
            setTextMessage(String.format("%s%s", message.substring(0, mention.getStartInMessage()), message.substring(mention.getEndInMessage() + 1)));
            mEditTextMessage.setSelection(mention.getStartInMessage());
        } else {
            changeListMentionInfoAfterDeleteCharacters(mEditTextMessage.getSelectionStart(), 1);
            // Because this method be called before afterTextChanged called -> in that method, this flag enable to reset the text
            cursorIndexForReformatMessage = mEditTextMessage.getSelectionStart() == 0 ? 0 : mEditTextMessage.getSelectionStart() - 1;
            reformatMessage = true;
        }
//        Log.d("aaaa", "delete mention start = " + mention.getStartInMessage() + ", end = " + mention.getEndInMessage());
    }

    private void deleteMentionByAddingCharacter(Mention mention, String characterAdded) {
        preventActionAfterSetText = true;
        changeListMentionInfoAfterAddCharacters(mEditTextMessage.getSelectionStart(), 1);
        if (!characterAdded.equals(" ")) {
            mMentions.remove(mention);
            reformatMessage = true;
            // Because this method be called before afterTextChanged called -> in that method, this flag enable to reset the text
            cursorIndexForReformatMessage = mEditTextMessage.getSelectionStart();
        }
    }

    private void logAllMentions(String a) {
        for (Mention mention : mMentions) {
//            Log.d("aaaa", a + " ***** mention start = " + mention.getStartInMessage() + ", end = " + mention.getEndInMessage());
        }
    }

    private Mention isThereMentionHere(int cursorIndex) {
//        Log.d("aaaa", "isThereMentionHere: cursor = " + cursorIndex);
        for (Mention mention : mMentions) {
            if (mention.getStartInMessage() < cursorIndex && cursorIndex <= mention.getEndInMessage() + 1) {
                return mention;
            }
        }
        return null;
    }

    private void changeListMentionInfoAfterDeleteCharacters(int cursorIndex, int numberOfCharacters) {
        for (Mention mention : mMentions) {
            if (mention.getStartInMessage() >= cursorIndex) {
                mention.setStartInMessage(mention.getStartInMessage() - numberOfCharacters);
                mention.setEndInMessage(mention.getEndInMessage() - numberOfCharacters);
            }
        }
        logAllMentions("when delete character");
    }

    private void changeListMentionInfoAfterAddCharacters(int cursorIndex, int numberOfCharacters) {
        for (Mention mention : mMentions) {
            if (mention.getStartInMessage() >= cursorIndex) {
                mention.setStartInMessage(mention.getStartInMessage() + numberOfCharacters);
                mention.setEndInMessage(mention.getEndInMessage() + numberOfCharacters);
            }
        }
        logAllMentions("when add character");
    }

    private void changeListMentionInfoAfterAddMention(int cursorIndex, Mention newAddedMention) {
//        Log.d("aaaa", "changeListMentionInfoAfterAddMention: cursor index = " + cursorIndex);
        int numberOfAddedCharacter = newAddedMention.getEndInMessage() - newAddedMention.getStartInMessage() + MENTION_TAIL.length();
//        Log.d("aaaa", "changeListMentionInfoAfterAddMention: numberOfAddedCharacter = " + numberOfAddedCharacter);

        for (Mention mention : mMentions) {
            if (mention.getStartInMessage() >= cursorIndex) {
//                Log.d("aaaa", "changeListMentionInfoAfterAddMention: in loop for mention start = " + mention.getStartInMessage());

                mention.setStartInMessage(mention.getStartInMessage() + numberOfAddedCharacter);
                mention.setEndInMessage(mention.getEndInMessage() + numberOfAddedCharacter);
            }
        }
    }

    private void setTextMessage(String text) {
        SpannableString spannableString = new SpannableString(text);
        for (Mention mention : mMentions) {
            try {
                int start = mention.getStartInMessage();
                int end = mention.getEndInMessage() + 1;
                if (start < 0 || end > text.length() || end <= start) {
                    continue;
                }
                formatMention(spannableString, start, end);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mEditTextMessage.setText(spannableString);
    }

    public void setFormattedMessage(String fullMessage) {
        preventCheckOpenMentionList = true;
        preventActionAfterSetText = true;
        setTextMessage(fullMessage);
    }

    public SpannableString getFormattedMessage() {
        String text = mEditTextMessage.getText().toString();
        SpannableString spannableString = new SpannableString(text);
        for (Mention mention : mMentions) {
            try {
                int start = mention.getStartInMessage();
                int end = mention.getEndInMessage() + 1;
                if (start < 0 || end > text.length() || end <= start) {
                    continue;
                }
                formatMention(spannableString, start, end);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return spannableString;
    }

    private void showMentionList(int cursorIndex) {
        cursorBeginTextSearch = cursorIndex;
        mState = STATE_SHOW_MENTION_LIST;
        if (mActionListener != null) {
            mActionListener.onNeedOpenListMention();
        }
    }

    public void hideMentionList() {
        mState = STATE_NONE;
        if (mActionListener != null) {
            mActionListener.onNeedCloseListMention();
        }
    }

    public void setActionListener(ActionListener actionListener) {
        mActionListener = actionListener;
    }

    public boolean hasMention() {
        return mMentions.size() > 0;
    }

    private void formatMention(SpannableString spannableString, int start, int end) {
        Context context;
        if (mEditTextMessage == null) {
            context = mContext;
        } else {
            context = mEditTextMessage.getContext();
        }
        spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
    }

    public void clearMentions() {
        mMentions.clear();
    }

    public void setMentions(List<Mention> mentions) {
        mMentions.clear();
        mMentions.addAll(mentions);
    }

    public interface ActionListener {
        void onNeedOpenListMention();

        void onNeedCloseListMention();

        void onSearchListMention(String keyword);
    }
}