/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws.measures;

import de.uros.citlab.errorrate.types.KWS.Match;
import de.uros.citlab.errorrate.types.KWS.MatchList;
import de.uros.citlab.errorrate.types.KWS.Type;

/**
 *
 * @author tobias
 */
public abstract class NDCG implements IRankingMeasure {

    public double calcNDCG(MatchList matches) {
        if (matches.matches.size() == 0) {
            return 1.0;
        }
        matches.sort();

        double sum = 0.0;
        double z = 0.0;
        int count = 0;

        for (Match match : matches.matches) {
            count++;
            double val = 1 / Math.log(count + 1);
            sum += (match.type == Type.TRUE_POSITIVE ? 1 : -1) * val;
            z += val;
        }

        return sum / z;

    }

}
