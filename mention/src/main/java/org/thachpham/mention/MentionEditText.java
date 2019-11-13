package org.thachpham.mention;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

public class MentionEditText extends AppCompatEditText {
    private final int STATE_NONE = 0;
    private final int STATE_SHOW_MENTION_LIST = 1;
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

    // Attrs
    private int mentionColor;
    private int mentionStyle = Typeface.BOLD;

    private Context mContext;

    public MentionEditText(Context context) {
        super(context);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttrs(attrs);
        init();
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAttrs(attrs);
        init();
    }

    private void setAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MentionEditText);
        try {
            mentionColor = a.getColor(R.styleable.MentionEditText_mentionColor, ContextCompat.getColor(getContext(), R.color.mention_color_default));
            mentionStyle = a.getInt(R.styleable.MentionEditText_mentionStyle, Typeface.BOLD);
            denotationCharacter = a.getString(R.styleable.MentionEditText_denotation);
            if ("".equals(denotationCharacter)) denotationCharacter = "@";
        } finally {
            a.recycle();
        }
    }

    private void init() {
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
                String message = getText().toString();
                int denotationIndex = getStartIndexOfDenotation();
                int currentSelector = getSelectionEnd();

                switch (mState) {
                    case STATE_NONE:
                        if (denotationIndex != -1 && isThereMentionHere(currentSelector) == null) {
                            showMentionList(denotationIndex + 1);
                            if (mActionListener != null) {
                                if(!mActionListener.onSearchListMention(message.substring(denotationIndex + 1, currentSelector))) {
                                    hideMentionList();
                                }
                            }
                        } else {
                            hideMentionList();
                        }
                        break;
                    case STATE_SHOW_MENTION_LIST:
                        if (denotationIndex == -1) {
                            hideMentionList();
                        } else {
                            if (currentSelector == cursorBeginTextSearch - 1) {
                                hideMentionList();
                            } else if (currentSelector >= denotationIndex) {
                                if (mActionListener != null) {
                                    if (!mActionListener.onSearchListMention(message.substring(denotationIndex + 1, currentSelector).toLowerCase())) {
                                        hideMentionList();
                                    }
                                }
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
                Mention mention = isThereMentionHere(getSelectionStart());
                if (after > count) {
                    if (mention != null) {
                        mentionWillBeDeleted = mention;
                        deleteMentionByAddingCharacter = true;
                    } else {
                        changeListMentionInfoAfterAddCharacters(getSelectionStart(), after - count);
                    }
                } else if (after < count) {
                    if (mention != null) {
                        deleteMention(mention, false);
                    } else {
                        changeListMentionInfoAfterDeleteCharacters(getSelectionStart(), count - after);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                Log.d(TAG, "aaaaa: start = " + start + ", count = " + count + ", before = " + before + ", s = " + s);
//                Log.d("aaaa", "onTextChanged: cursor start = " + getSelectionStart() + ", cursor end = " + getSelectionEnd());
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
                    preventCheckOpenMentionList = true;
                    setTextMessage(getText().toString());
                    try {
                        setSelection(cursorIndexForReformatMessage);
                    } catch (Exception e) {
                        setSelection(getText().toString().length());
                    }
                }
            }
        });
    }

    /**
     * Method add denotation "@name" to the message
     */
    public void addMention(MentionInput mentionInput) {
        hideMentionList();
        preventActionAfterSetText = true;
        preventCheckOpenMentionList = true;
        String currentMessage = getText().toString();
        Log.d("aaaa", "current message = " + currentMessage);

        int cursorPosition = getSelectionStart();
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
        setSelection(cursorBeginTextSearch + mention.getMentionText().length() + MENTION_TAIL.length());
    }

    public void addDenotation() {
        if(mActionListener != null) {
            mActionListener.onNeedOpenListMention();
        }
        String message = getText().toString();
        if (message.length() > 0) {
            int currentStartIndex = getSelectionStart();
            String previousString = message.substring(currentStartIndex - 1, currentStartIndex);
            if (!" ".equals(previousString))
                addASpace();
        }
        addJustDenotation();
    }

    public void addJustDenotation() {
        preventActionAfterSetText = true;
        preventCheckOpenMentionList = true;
        int cursorIndex = getSelectionStart();
        cursorBeginTextSearch = cursorIndex + 1;
        mState = STATE_SHOW_MENTION_LIST;
        Mention mentionHere = isThereMentionHere(cursorIndex);
        if (mentionHere != null) {
            mMentions.remove(mentionHere);
        }
        int cursorEndIndex = getSelectionEnd();
        int lengthChange = cursorEndIndex - cursorIndex + 1;
        String message = getText().toString();
        message = message.substring(0, cursorIndex) + denotationCharacter + message.substring(cursorEndIndex);
        changeListMentionInfoAfterDeleteCharacters(cursorIndex, lengthChange - 1);
        changeListMentionInfoAfterAddCharacters(cursorIndex, 1);
        setTextMessage(message);
        setSelection(cursorIndex + 1);
    }

    private void addASpace() {
//        deleteMentionByAddingCharacter = false;
        preventActionAfterSetText = true;
        preventCheckOpenMentionList = true;
        int cursorIndex = getSelectionStart();
        cursorBeginTextSearch = cursorIndex + 1;
        mState = STATE_SHOW_MENTION_LIST;
        int cursorEndIndex = getSelectionEnd();
        int lengthChange = cursorEndIndex - cursorIndex + 1;
        String message = getText().toString();
        message = message.substring(0, cursorIndex) + " " + message.substring(cursorEndIndex);
        changeListMentionInfoAfterDeleteCharacters(cursorIndex, lengthChange - 1);
        changeListMentionInfoAfterAddCharacters(cursorIndex, 1);
        setTextMessage(message);
        setSelection(cursorIndex + 1);
    }

    private int getStartIndexOfDenotation() {
        String message = getText().toString();
        int currentIndex = getSelectionStart() - 1;
        for (int i = currentIndex; i >= 0; i--) {
            if (isDenotation(String.valueOf(message.charAt(i)))) {
                if (i == 0) {
                    return i;
                } else {
                    if (" ".equals(String.valueOf(message.charAt(i - 1))) || "\n".equals(String.valueOf(message.charAt(i - 1)))) {
                        return i;
                    }
                }
                return -1;
            }
        }

        return -1;
    }

    private void deleteMention(Mention mention, boolean deleteAllTextInMention) {
        preventActionAfterSetText = true;
        mMentions.remove(mention);
        String message = getText().toString();
        if (deleteAllTextInMention) {
            changeListMentionInfoAfterDeleteCharacters(getSelectionStart(), mention.getMentionText().length() + 1);
            setTextMessage(String.format("%s%s", message.substring(0, mention.getStartInMessage()), message.substring(mention.getEndInMessage() + 1)));
            setSelection(mention.getStartInMessage());
        } else {
            changeListMentionInfoAfterDeleteCharacters(getSelectionStart(), 1);
            // Because this method be called before afterTextChanged called -> in that method, this flag enable to reset the text
            cursorIndexForReformatMessage = getSelectionStart() == 0 ? 0 : getSelectionStart() - 1;
            reformatMessage = true;
        }
//        Log.d("aaaa", "delete mention start = " + mention.getStartInMessage() + ", end = " + mention.getEndInMessage());
    }

    private void deleteMentionByAddingCharacter(Mention mention, @NonNull String characterAdded) {
        preventActionAfterSetText = true;
        changeListMentionInfoAfterAddCharacters(getSelectionStart(), 1);
        if (!characterAdded.equals(" ")) {
            mMentions.remove(mention);
            reformatMessage = true;
            // Because this method be called before afterTextChanged called -> in that method, this flag enable to reset the text
            cursorIndexForReformatMessage = getSelectionStart();
        }
    }

    private void logAllMentions(String a) {
        for (Mention mention : mMentions) {
//            Log.d("aaaa", a + " ***** mention start = " + mention.getStartInMessage() + ", end = " + mention.getEndInMessage());
        }
    }

    @Nullable
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

    private void changeListMentionInfoAfterAddMention(int cursorIndex, @NonNull Mention newAddedMention) {
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
        setText(spannableString);
    }

    public void setFormattedMessage(String fullMessage) {
        preventCheckOpenMentionList = true;
        preventActionAfterSetText = true;
        setTextMessage(fullMessage);
    }

    public SpannableString getFormattedMessage() {
        String text = getText().toString();
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

    private MentionSort mentionSort = new MentionSort();

    private void formatMention(SpannableString spannableString, int start, int end) {
//        Log.d("aaaa", "formatMention: mMetion size = " + mMentions.size());
//        logAllMentions("*******");
        Context context;
        spannableString.setSpan(new ForegroundColorSpan(mentionColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(mentionStyle), start, end, 0);
    }

    private boolean isDenotation(String symbol) {
        return denotationCharacter.equals(symbol);
    }

    private boolean isDenotationWithSpace(String compareString) {
        return (" " + denotationCharacter).equals(compareString);
    }

    private boolean isDenotationWithBreak(String compareString) {
        return ("\n" + denotationCharacter).equals(compareString);
    }

    private class MentionSort implements Comparator<Mention> {

        @Override
        public int compare(@NonNull Mention o1, @NonNull Mention o2) {
            return o1.getStartInMessage() - o2.getStartInMessage();
        }
    }

    public interface ActionListener {
        void onNeedOpenListMention();

        void onNeedCloseListMention();

        /**
         * Callback method for searching by keyword
         *
         * @param keyword
         * @return true if result found, false if result empty
         */
        boolean onSearchListMention(String keyword);
    }
}
