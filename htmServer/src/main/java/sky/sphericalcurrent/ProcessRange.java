package sky.sphericalcurrent;

import spherical.Trixel;
import spherical.refs.Cartesian;
import spherical.util.Pair;
import spherical.util.Triple;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ProcessRange {

    public static void main(String[] args) throws Exception {

        //String polygon = "CHULL LATLON 41.586419 28.148929 41.250697 29.034702 41.142196 29.910862 41.013831
        // 29.822971 41.005540 29.718601 40.960962 29.671909 40.815616 29.342319 40.836399 29.118473 40.951628
        // 28.661167 41.060444 28.278019 41.034552 28.017093 41.081150 27.969028 41.246567 27.999240 41.510365
        // 28.07065 ";
//        String shape = "POLY LATLON 41.588474 28.153049 41.130819 29.905368 40.778189 29.438449 40.877946 28.581516 41.006577 27.977268 41.324994 28.021213";
//        shape = "CIRCLE J2000 195 0 1";
//        shape = "POLY LATLON 41.609013 28.389255 41.060444 29.844944 40.719928 28.098117 41.258956 27.768528";
//        shape = "RECT LATLON 37 -109.55  41 -102.05";
//        Region reg = Parser.compile(shape);
//        System.out.println(reg.toString());
//        reg.Simplify(); //WAS:true, Limits.Default.Tolerance, Const.MaximumIteration, false);
//        System.out.println(reg.toString());
//        List<Pair<Long, Long>> table = Cover.HidRange(reg);
//        System.out.println(table.size());

        double lat = 41.545968;
        double lon = 29.190186;
        List<Pair<Long, Long>> hidRanges = parseShapeHidRangesIstanbul();
        checkHid(lat, lon, hidRanges);
    }

    public static long fHtmLatLon(double lat, double lon, int depth) throws Exception {
        Triple<Double, Double, Double> res = Cartesian.Radec2Xyz(lon, lat);
        return Trixel.CartesianToHid(res.getX(), res.getY(), res.getZ(), depth);
    }

    public static void checkHid(double lat, double lon, List<Pair<Long, Long>> hidRanges) throws Exception {
        long hid = fHtmLatLon(lat, lon, 20);
        System.out.println(hid);
        isInside(hidRanges, hid).ifPresent(pair -> {
            System.out.println("INSIDE");
            System.out.println(pair.getX() + " " + pair.getY());
        });
    }

    public static Optional<Pair<Long, Long>> isInside(List<Pair<Long, Long>> hidRanges, long hid) {
        return hidRanges.stream().filter(pair -> hid <= pair.getX() && hid >= pair.getY()).findAny();
    }

    public static List parseShapeHidRangesIstanbul() {
        String shapeHidList = "17369187483647 17369183289344\n" +
                "\n" +
                "17372412903423 17372404514816\n" +
                "\n" +
                "17372421292031 17372417097728\n" +
                "\n" +
                "17372433874943 17372429680640\n" +
                "\n" +
                "17372467429375 17372463235072\n" +
                "\n" +
                "17372744253439 17372740059136\n" +
                "\n" +
                "17372756836351 17372748447744\n" +
                "\n" +
                "17372891054079 17372874276864\n" +
                "\n" +
                "17372941385727 17372895248384\n" +
                "\n" +
                "17372966551551 17372962357248\n" +
                "\n" +
                "17373033660415 17373025271808\n" +
                "\n" +
                "17373042049023 17373037854720\n" +
                "\n" +
                "17373054631935 17373050437632\n" +
                "\n" +
                "17373063020543 17373058826240\n" +
                "\n" +
                "17373075603455 17373071409152\n" +
                "\n" +
                "17373109157887 17373079797760\n" +
                "\n" +
                "17373142712319 17373113352192";

        return Arrays.asList(shapeHidList.split("\n")).stream()
                .filter(row -> row != null && row.length()>0)
                .map(row -> {
            String[] hidRange = row.split(" ");
            return new Pair(Long.parseLong(hidRange[0]), Long.parseLong(hidRange[1]));
        }).collect(toList());
    }
}
