package de.uros.citlab.errorrate.interfaces;


import java.awt.*;

public interface ILine {
    /**
     * transcription of text line. Text should not be null, empty or starting/ending with spaces
     *
     * @return
     */
    String getText();

    /**
     * baseline which goes from left to right under the main body of the text (descenders can to deeper).
     * If no baseline is available, return null
     *
     * @return
     */
    Polygon getBaseline();
}
