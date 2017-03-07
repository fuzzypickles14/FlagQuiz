package com.deitel.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.deitel.flagquiz.MainActivityFragment.FLAGS_IN_QUIZ;

/**
 * Created by Andrew Toomey on 3/6/2017.
 */

public class FlagAndButtonFragment extends Fragment {
    private static final String TAG = "FlagQuiz Activity";
    private LinearLayout quizLinearLayout; // layout that contains the quiz
    private ImageView flagImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView;
    private TextView questionNumberTextView;

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // countries in current quiz
    private Set<String> regionsSet; // world regions in current quiz
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int questionNumber;
    private int guessForQuestion;
    private int guessRows; // number of rows displaying guess Buttons
    private SecureRandom random; // used to randomize the quiz
    private Animation shakeAnimation; // animation for incorrect guess

    private boolean comingBack = false;



    @Override
    public void onStart() {
        super.onStart();
        if (comingBack) {
            if (questionNumber == FLAGS_IN_QUIZ) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                loadCurrentQuestionFromPreferences(preferences);
                showGameOverMessage();
            }else {
                initFlags();
                loadNextFlag();
            }
        }

    }

    private void showGameOverMessage() {
        // DialogFragment to display quiz stats and start new quiz
        DialogFragment quizResults =
                new DialogFragment() {
                    // create an AlertDialog and return it
                    @Override
                    public Dialog onCreateDialog(Bundle bundle) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(getActivity());
                        builder.setMessage(
                                getString(R.string.results,
                                        totalGuesses,
                                        (1000 / (double) totalGuesses)));

                        // "Reset Quiz" Button
                        builder.setPositiveButton(R.string.reset_quiz,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        resetQuiz();
                                    }
                                }
                        );

                        return builder.create(); // return the AlertDialog
                    }
                };

        // use FragmentManager to display the DialogFragment
        quizResults.setCancelable(false);
        quizResults.show(getFragmentManager(), "quiz results");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceStat) {
        View view = inflater.inflate(R.layout.fragment_flag_and_button, container, false);
            fileNameList = new ArrayList<>();
            quizCountriesList = new ArrayList<>();
            random = new SecureRandom();
            questionNumberTextView = (TextView) container.findViewById(R.id.questionNumberTextView);
            shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                    R.anim.incorrect_shake);
            shakeAnimation.setRepeatCount(3);

            initTextAndImageViews(view);
            initButtons(view);
            configureButtonListeners();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            updateGuessRows(preferences);
            updateRegions(preferences);

        return view;
    }


    private void initTextAndImageViews(View view) {
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
        answerTextView.setText("");


        quizLinearLayout =
                (LinearLayout) view.findViewById(R.id.flagAndButtonDetailLinearLayout);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
    }
    private void configureButtonListeners() {
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }
    }
    private void initButtons(View view) {
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] =
                (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] =
                (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] =
                (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] =
                (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
    }
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get the number of guess buttons that should be displayed
        String choices =
                sharedPreferences.getString(MainActivity.CHOICES, null);

        guessRows = Integer.parseInt(choices) / 2;
        // hide all quess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        // display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }
    public void updateRegions(SharedPreferences sharedPreferences) {
        regionsSet =
                sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    public void signalComingBack() {
        comingBack = true;
    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String region = correctAnswer.substring(0, correctAnswer.indexOf("-"));
            String answer = getCountryName(correctAnswer);
            ++totalGuesses; // increment number of guesses the user has made
            guessForQuestion++;

            if (guess.equals(answer)) { // if the guess is correct
                ++correctAnswers; // increment the number of correct answers
                questionNumber++;
                guessForQuestion = 0;

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(
                        getResources().getColor(R.color.correct_answer,
                                getContext().getTheme()));

                disableButtons(); // disable all guess Buttons
                goToFlagDetailFragment(region, true);
            }
            else { // answer was incorrect
                answerWasIncorrect(guessButton, guess, region);
                if (guessForQuestion == Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(MainActivity.NUMGUESSES, null))) {
                    guessForQuestion = 0;
                    questionNumber++;
                    answerTextView.setText(String.format("The correct answer is %s!", answer));
                    answerTextView.setTextColor(getResources().getColor(
                            R.color.incorrect_answer, getContext().getTheme()));
                    goToFlagDetailFragment(region, false);
                }
            }
        }
    };

    private void goToFlagDetailFragment(String region, Boolean isCorrect) {
        comingBack = false;
        FlagDetailFragment frag = new FlagDetailFragment();
        Bundle args = new Bundle();
        args.putString("Region", region);
        args.putString("Country", getCountryName(correctAnswer));
        args.putBoolean("Correct", isCorrect);
        frag.setArguments(args);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_fragment_container, frag).addToBackStack(null);
        transaction.commit();
    }

    private void answerWasIncorrect(Button guessButton, String guess, String region) {
        flagImageView.startAnimation(shakeAnimation); // play shake

        // display "Incorrect!" in red
        answerTextView.setText(R.string.incorrect_answer);
        answerTextView.setTextColor(getResources().getColor(
                R.color.incorrect_answer, getContext().getTheme()));
        guessButton.setEnabled(false); // disable incorrect answer
        setButtonImage(guessButton, guess, region);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        String currentQuestion = preferences.getString(MainActivity.CURRENTQUESTION, null);
        currentQuestion = currentQuestion.replace(String.format(",%s:T", guess), String.format(",%s:F", guess));
        currentQuestion = currentQuestion.replaceAll("TotalGuess:\\d+", String.format("TotalGuess:%d", totalGuesses));
        editor.putString(MainActivity.CURRENTQUESTION, currentQuestion);
        editor.apply();
    }
    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
    public void resetQuiz() {
        // use AssetManager to get image file names for enabled regions

        questionNumber = 0;
        correctAnswers = 0; // reset the number of correct answers made
        totalGuesses = 0; // reset the total number of guesses the user made
        quizCountriesList.clear(); // clear prior list of quiz countries


        initFlags();
        loadNextFlag();
    }
    private void initFlags() {
        addFlagsToList();
        addFlagsToQuiz();
    }
    private void addFlagsToQuiz() {
        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // add FLAGS_IN_QUIZ random file names to the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            // get the random file name
            String filename = fileNameList.get(randomIndex);

            // if the region is enabled and it hasn't already been chosen
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename); // add the file to the list
                ++flagCounter;
            }
        }
    }
    private void addFlagsToList() {
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // empty list of image file names

        try {
            // loop through each region
            for (String region : regionsSet) {
                // get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }
    }
    private ArrayList<String> filterByRegion(ArrayList<String> allCountries, String region) {
        ArrayList<String> temp = new ArrayList<>();
        for (String country: allCountries) {
            if (country.substring(0, country.indexOf("-")).equals(region)) {
                temp.add(country);
            }
        }
        return temp;
    }
    public void loadNextFlag() {
        // get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; // update the correct answer

        setQuestionNumber();

        // extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        // use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        // get an InputStream to the asset representing the next flag
        // and try to use the InputStream
        try (InputStream stream =
                     assets.open(region + "/" + nextImage + ".png")) {
            // load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            //animate(false); // animate the flag onto the screen
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList);// shuffle file names
        ArrayList<String> unfilteredFileNameList = (ArrayList<String>) fileNameList;

        // put the correct answer at the end of fileNameList
        int correct = unfilteredFileNameList.indexOf(correctAnswer);
        unfilteredFileNameList.add(unfilteredFileNameList.remove(correct));

        fileNameList = filterByRegion((ArrayList<String>) fileNameList, region);

        // add 2, 4, 6 or 8 guess Buttons based on the value of guessRow
        for (int row = 0; row < guessRows; row++) {
            // place Buttons in currentTableRow
            for (int column = 0;
                 column < guessLinearLayouts[row].getChildCount();
                 column++) {
                // get reference to Button to configure
                Button newGuessButton =
                        (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
                newGuessButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            }
        }

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
        fileNameList = unfilteredFileNameList;
        setupCurrentQuestionData(nextImage);
    }
    private void setupCurrentQuestionData(String answer) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        String currentQuestion = "";
        currentQuestion = currentQuestion.concat(answer);
        LinearLayout randomRow;
        for (int i = 0; i < guessRows; i++) {
            randomRow = guessLinearLayouts[i];
            for (int j = 0; j < 2; j++) {
                currentQuestion = currentQuestion.concat(String.format(",%s:T",(String)((Button) randomRow.getChildAt(j)).getText()));
            }
        }
        currentQuestion = currentQuestion.concat(String.format(",QuestionNum:%d,CorrectAnswers:%d,TotalGuess:%d", questionNumber, correctAnswers, totalGuesses));
        editor.putString(MainActivity.CURRENTQUESTION, currentQuestion);
        editor.apply();
    }
    public void loadCurrentQuestionFromPreferences(SharedPreferences preferences) {
        initFlags();
        String currentQuestionData = preferences.getString(MainActivity.CURRENTQUESTION, null);
        String data[] = currentQuestionData.split(",");
        String nextImage = data[0] + ".png";

        correctAnswer = data[0];
        questionNumber = Integer.parseInt(data[data.length - 3].substring(data[data.length - 3].indexOf(":") + 1));
        correctAnswers = Integer.parseInt(data[data.length - 2].substring(data[data.length - 2].indexOf(":") + 1));
        totalGuesses = Integer.parseInt(data[data.length - 1].substring(data[data.length - 1].indexOf(":") + 1));
        setQuestionNumber();

        String region = data[0].substring(0, data[0].indexOf('-'));
        AssetManager assets = getActivity().getAssets();

        try (InputStream stream =
                     assets.open(region + "/" + nextImage)) {
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        int rowIndex = 0;
        for (int row = 0; row < guessRows; row++) {
            for (int column = 0;
                 column < guessLinearLayouts[row].getChildCount();
                 column++) {
                Button newGuessButton =
                        (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);
                int buttonTextIndex = row + column + 1 + rowIndex;
                newGuessButton.setText(data[buttonTextIndex].substring(0, data[buttonTextIndex].indexOf(":")));
                if (data[buttonTextIndex].contains(":T")) {
                    newGuessButton.setEnabled(true);
                    newGuessButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
                } else {
                    newGuessButton.setEnabled(false);
                    setButtonImage(newGuessButton, (String)newGuessButton.getText(), region);
                }
            }
            rowIndex++;
        }

    }

    private void setButtonImage(Button button, String country, String region) {

        AssetManager assets = getActivity().getAssets();
        country = country.replace(" ", "_");
        String countryAndRegion = String.format("%s-%s", region, country);
        try (InputStream stream =
                     assets.open(String.format("%s/%s.png", region, countryAndRegion))) {

            Drawable flag = Drawable.createFromStream(stream, countryAndRegion);

            button.setCompoundDrawablesRelativeWithIntrinsicBounds(flag, null, null, null);
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + countryAndRegion, exception);
        }
    }

    private void setQuestionNumber() {
        answerTextView.setText(""); // clear answerTextView

        // display current question number
        questionNumberTextView.setText(getString(
                R.string.question, questionNumber + 1, FLAGS_IN_QUIZ));

    }




}