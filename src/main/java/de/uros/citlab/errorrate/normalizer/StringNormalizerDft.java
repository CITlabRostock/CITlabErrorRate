/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.normalizer;

import eu.transkribus.interfaces.IStringNormalizer;
import de.uros.citlab.tokenizer.categorizer.CategoryUtils;

import java.text.Bidi;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalize the String using some unicode methods. The
 *
 * @author gundram
 */
public class StringNormalizerDft implements IStringNormalizer {

    public static Logger LOG = LoggerFactory.getLogger(StringNormalizerDft.class.getName());
    private Normalizer.Form form;
    public Map<String, String> categoryMap = new HashMap<>();
    private boolean toUpper;

    public StringNormalizerDft(Form form, boolean toUpper) {
        this.form = form;
        this.toUpper = toUpper;
    }

    public StringNormalizerDft() {
        this(Form.NFKC, false);
    }

    public void setForm(Normalizer.Form form) {
        this.form = form;
    }

    public void setToUpper(boolean toUpper) {
        this.toUpper = toUpper;
    }

    @Override
    public String normalize(String string) {
        if (form != null) {
            string = Normalizer.normalize(string, form);
        }
        if (toUpper) {
            string = string.toUpperCase(Locale.ROOT);
        }
        if (form == null) {
            return string;
        }
        char[] chars = string.toCharArray();
//        boolean requiresBidi = sun.text.bidi.BidiBase.requiresBidi(chars, 0, chars.length);
        if (Bidi.requiresBidi(chars, 0, chars.length)) {
            LOG.warn("have to use bidirectional scanning - not implemented accurate so far.");
        }
        StringBuilder sb = new StringBuilder();

        for (char aChar : chars) {
            switch (Character.getType(aChar)) {
                // not shure how to handle type
                case Character.MODIFIER_LETTER://Lm
                case Character.MODIFIER_SYMBOL://Sk
                case Character.PRIVATE_USE://Co
                    LOG.warn("unsure about sign " + Character.getName(aChar) + " from type " + Character.getType(aChar) + " and category " + CategoryUtils.getCategory(aChar) + ", leave it how it is ('" + string + "')");
                //normal category - leave it how it is
                case Character.COMBINING_SPACING_MARK://Mc
                case Character.CONNECTOR_PUNCTUATION://Pc
                case Character.CURRENCY_SYMBOL://Sc
                case Character.DASH_PUNCTUATION://Pd
                case Character.DECIMAL_DIGIT_NUMBER://Nd
                case Character.END_PUNCTUATION://Pe
                case Character.FINAL_QUOTE_PUNCTUATION://Pf
                case Character.INITIAL_QUOTE_PUNCTUATION://Pi
                case Character.LETTER_NUMBER://Nl
                case Character.LOWERCASE_LETTER://Ll
                case Character.MATH_SYMBOL://Sm
                case Character.OTHER_LETTER://Lo
                case Character.OTHER_PUNCTUATION://Po
                case Character.OTHER_SYMBOL://So
                case Character.SPACE_SEPARATOR://Zs
                case Character.START_PUNCTUATION://Ps
                case Character.TITLECASE_LETTER://Lt
                case Character.UPPERCASE_LETTER://Lu
                case Character.OTHER_NUMBER://No
                    sb.append(aChar);
                    break;
                //characters, which should not appear - delete them
                case Character.CONTROL://Cc
                    if (aChar == '\n') {
                        LOG.warn("found char " + Character.getName(aChar) + ", because it is of category " + CategoryUtils.getCategory(aChar) + " ('" + string + "'), will stay into string.");
                        sb.append(aChar);
                        break;
                    }
                case Character.ENCLOSING_MARK://Me
                case Character.FORMAT://Cf
                case Character.LINE_SEPARATOR://Zl
                case Character.NON_SPACING_MARK://Mn
                case Character.PARAGRAPH_SEPARATOR://Zp
                case Character.SURROGATE://Cs
                    LOG.warn("ignore sign " + Character.getName(aChar) + ", because it is of category " + CategoryUtils.getCategory(aChar) + " ('" + string + "')");
                    break;
                case Character.UNASSIGNED://Cn
                default:
                    LOG.warn("no category found for " + Character.getName(aChar) + " - maybe bug in code.");
                    sb.append(aChar);
            }
        }
        return sb.toString();
    }
}
