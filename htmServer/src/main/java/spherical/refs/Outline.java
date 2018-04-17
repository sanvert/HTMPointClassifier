package spherical.refs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Outline {
    //
    // Static Fields
    //
    private static final Double eps = Constant.Tolerance / 100.0;

    public static final String Revision = "$Revision: 1.26 $";

    //
    // Fields
    //
    private List<IPatch> patchList;

    private List<PatchPart> partList;

    private Double length;

    private Map<Halfspace, Map<Integer, List<DArc>>> circleArcs = new HashMap<>();

    public Double Length()

    {
        return this.length;
    }

    public List<PatchPart> PartList()

    {
        return this.partList;
    }

    public List<IPatch> getPatchList() {
        return patchList;
    }

    //
    // Constructors
    //
    public Outline(List<IPatch> patches) throws Exception {
        this.patchList = patches;
        this.partList = new ArrayList<>();
        for(IPatch current : patches) {
            PatchPart patchPart = new PatchPart();
            patchPart.setMec(current.mec());
            this.partList.add(patchPart);
        }
        this.Build();
    }

    //
    // Static Methods
    //
    private static List<DArcChange> BuildChangeList(Arc full, Arc fullinv, List<DArc> lpos, List<DArc> lneg) {
        lpos.get(0).angleStart = 0.0;
        lpos.get(0).angleEnd = lpos.get(0).arc.Angle();
        for (int i = 1; i < lpos.size(); i++) {
            DArc dArc = lpos.get(i);
            dArc.angleStart = full.GetAngle(dArc.arc.getPoint1());
            dArc.angleEnd = dArc.angleStart + dArc.arc.Angle();
        }
        for(DArc current : lneg) {
            if (current.arc.IsFull()) {
                current.angleStart = 0.0;
                current.arc = fullinv;
                current.angleEnd = current.arc.Angle();
            } else {
                current.angleStart = full.GetAngle(current.arc.getPoint2());
                current.angleEnd = current.angleStart + current.arc.Angle();
                if (current.angleStart >= 2 * Math.PI) {
                    current.angleStart -= 2 * Math.PI;
                    current.angleEnd -= 2 * Math.PI;
                }
            }
        }
        lpos.sort(DArc::ComparisonAngle);
        lneg.sort(DArc::ComparisonAngle);

        double num = -1.0;
        double num2 = 10.0 * Constant.DoublePrecision;
        for(DArc current2 : lpos) {
            if (current2.angleStart <= num) {
                current2.angleStart = num + num2;
            }
            if (current2.angleEnd <= current2.angleStart) {
                current2.angleEnd = current2.angleStart + num2;
            }
            num = current2.angleEnd;
        }
        num = -1.0;
        for(DArc current3 : lneg) {
            if (current3.angleStart <= num) {
                current3.angleStart = num + num2;
            }
            if (current3.angleEnd <= current3.angleStart) {
                current3.angleEnd = current3.angleStart + num2;
            }
            if (current3.angleEnd > 2 * Math.PI) {
                current3.angleEnd -= 2 * Math.PI;
                if (current3.angleEnd >= lneg.get(0).angleStart) {
                    current3.angleEnd = lneg.get(0).angleStart - num2;
                }
            }
            num = current3.angleEnd;
        }
        if (lneg.get(lneg.size() - 1).angleStart > lneg.get(lneg.size()-1).angleEnd &&
                lneg.get(0).angleStart <= lneg.get(lneg.size()-1).angleEnd) {
            lneg.get(lneg.size() - 1).angleEnd -= num2;
            if (lneg.get(lneg.size() - 1).angleEnd < 0.0) {
                lneg.get(lneg.size() - 1).angleEnd += 2 * Math.PI;
            }
        }
        List<DArcChange> list = new ArrayList<>(2 * (lpos.size() + lneg.size()));
        for(DArc current4 : lpos) {
            list.add(new DArcChange(current4, 1, current4.angleStart));
            list.add(new DArcChange(current4, -1, current4.angleEnd));
        }
        for(DArc current5 : lneg) {
            list.add(new DArcChange(current5, -1, current5.angleStart));
            list.add(new DArcChange(current5, 1, current5.angleEnd));
        }
        list.sort(DArcChange::comparisonAngle);
        return list;
    }

    //
    // Methods
    //
    private void Build() throws Exception {
        this.BuildCircleArcs();
        for(Map.Entry< Halfspace, Map<Integer, List<DArc>>> current : this.circleArcs.entrySet()){
            Halfspace key = current.getKey();
            Map<Integer, List<DArc>> value = current.getValue();
            List<DArc> list = value.get(1);
            List<DArc> list2 = value.get(-1);

            if (list.size() == 0) {
                list2.forEach(current2 -> {
                    current2.segment.getArcList().add(new Arc(current2.arc));
                });
                continue;
            }

            if (list2.size() == 0) {
                list.forEach(current3 -> {
                    current3.segment.getArcList().add(new Arc(current3.arc));
                });
                continue;
            }

            Cartesian point = list.get(0).arc.getPoint1();
            Arc full = new Arc(key, point);
            Arc fullinv = new Arc(key.Inverse(), point);
            List<DArcChange> dac = Outline.BuildChangeList(full, fullinv, list, list2);
            DArc lastNeg = list2.get(list2.size() - 1);
            this.Sweep(full, fullinv, dac, lastNeg);
        }
    }

    // Problematic
    private void BuildCircleArcs() {
        int num = 0;
        for(IPatch current : this.patchList){
            PatchPart segment = this.partList.get(num++);
            Arc[] arcList = current.getArcArray();
            for (int i = 0; i < arcList.length; i++) {
                Arc arc = arcList[i];
                Halfspace circle = arc.Circle();
                Halfspace key = circle.Inverse();
                if (this.circleArcs.containsKey(circle)) {
                    this.circleArcs.get(circle).get(1).add(new DArc(arc, 1, segment));
                } else if (this.circleArcs.containsKey(key)) {
                    this.circleArcs.get(key).get(-1).add(new DArc(arc, -1, segment));
                } else if (circle.getESign() == ESign.Negative) {
                    this.circleArcs.put(key, new HashMap<>());
                    this.circleArcs.get(key).put(1, new ArrayList<>());
                    this.circleArcs.get(key).put(-1, new ArrayList<>());
                    this.circleArcs.get(key).get(-1).add(new DArc(arc, -1, segment));
                } else {
                    this.circleArcs.put(circle, new HashMap<>());
                    this.circleArcs.get(circle).put(1, new ArrayList<>());
                    this.circleArcs.get(circle).put(-1, new ArrayList<>());
                    this.circleArcs.get(circle).get(1).add(new DArc(arc, 1, segment));
                }
            }
        }
    }

    private void Sweep(Arc full, Arc fullinv, List<DArcChange> dac, DArc lastNeg) throws Exception {
        Cartesian startpoint = full.getPoint1();

        // Init sweep params
        int sweep = 0, sweepStart = 0;
        DArc livePos = null, liveNeg = null;
        DArc liveArc = null;
        DArcChange dcA = null;
        DArcChange dcB;
        // check for overlapping negative
        if (lastNeg.angleEnd < lastNeg.angleStart)
        {
            sweep--;
            sweepStart--;
            liveNeg = lastNeg;
            liveArc = liveNeg;
            // start point is origo
            DArc cpNeg = new DArc(new Arc(lastNeg.arc), -1, lastNeg.segment);
            dcA = new DArcChange(cpNeg, -1, 0.0);
            dcA.darc.arc.setPoint2(startpoint);
        }

        double close = Constant.Tolerance / 1000.0; // Constant.TwoDoublePrecision;

        // loop
        DArcChange dc = null;
        for (int i = 0; i < dac.size(); i++)
        {
            dc = dac.get(i);

            sweep += dc.change;

            switch (sweep)
            {
                case 1:
                    if (dc.isStart()) livePos = dc.darc;
                    liveNeg = null;
                    liveArc = livePos;
                    dcA = dc;
                    break;
                case -1:
                    if (dc.isStart()) liveNeg = dc.darc;
                    livePos = null;
                    liveArc = liveNeg;
                    dcA = dc;
                    break;
                case 0:
                    dcB = dc;
                    boolean same = dcB.angle - dcA.angle < close;
                    //bool same = dcA.Point.Same(dcB.Point);
                    Arc a;
                    if (liveArc.dir == +1)
                    {
                        if (!same)
                        {
                            try
                            {
                                a = new Arc(full.Circle(), dcA.point(), dcB.point());
                                liveArc.segment.getArcList().add(a);
                            }
                            catch (IllegalArgumentException e) { }
                        }
                        if (dc.isStart()) liveNeg = dc.darc;
                        else livePos = null;
                    }
                    else
                    {
                        if (!same)
                        {
                            try
                            {
                                a = new Arc(fullinv.Circle(), dcB.point(), dcA.point());
                                liveArc.segment.getArcList().add(a);
                            }
                            catch (IllegalArgumentException e) { }
                        }
                        if (dc.isStart()) livePos = dc.darc;
                        else liveNeg = null;
                    }
                    liveArc = null;
                    break;

                default:
                    throw new Exception("Outline.Build(): invalid sweep value on circle\n\t " +  dc.darc.arc.Circle());
            }
        }
        if (sweep != sweepStart)
        {
            throw new Exception("Outline.Build(): sweepEnd != sweepStart");
        }
        if (sweep == -1)
        {
            boolean same = Math.abs(dc.angle - 2 * Math.PI) < close;
            //bool same = startpoint.Same(dc.Point);
            if (!same)
            {
                try {
                    Arc a = new Arc(fullinv.Circle(), startpoint, dc.point());
                    liveArc.segment.getArcList().add(a);
                }
                catch (IllegalArgumentException e) { }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(PatchPart current : this.partList){
            stringBuilder.append(current.toString());
        }
        return stringBuilder.toString();
    }
}