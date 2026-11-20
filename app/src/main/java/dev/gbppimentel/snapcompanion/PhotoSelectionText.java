package dev.gbppimentel.snapcompanion;

import java.util.*;

/** Independent owned-UI anchors for the folder overview and its inner photo picker. */
final class PhotoSelectionText {
    private static boolean strong(List<LiveReading.Word> words,String phrase){return CourseText.quality(words,phrase)>=.8f;}
    static boolean overviewControls(List<LiveReading.Word> words){
        return strong(words,"Evaluate Photos")&&(strong(words,"Auto")||strong(words,"Remove All"));
    }
    static boolean innerPicker(List<LiveReading.Word> words){
        return strong(words,"Photos Taken")&&strong(words,"Photodex")&&strong(words,"Selected")
            &&strong(words,"Compare")&&(strong(words,"All")||strong(words,"Total"));
    }
    static boolean highlightedOverview(List<LiveReading.Word> words){
        return overviewControls(words)&&words.stream().anyMatch(LiveReading.Word::selectionProbe);
    }
}
