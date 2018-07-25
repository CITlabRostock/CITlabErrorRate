/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws.measures;

import de.uros.citlab.errorrate.types.KWS.Match;
import de.uros.citlab.errorrate.types.KWS.MatchList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author tobias
 */
public class GlobalAveragePrecision extends AveragePrecision {

    @Override
    public double calcMeasure(List<MatchList> matchlists) {
        LinkedList<Match> list = new LinkedList<>();
        for (MatchList matchList : matchlists) {
            list.addAll(matchList.matches);
        }
        MatchList kwsMatchList = new MatchList(list);
        kwsMatchList.sort();
        return calcAveragePrecision(kwsMatchList);
    }

}
