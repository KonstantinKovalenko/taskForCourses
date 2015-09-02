package maintask2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import static mytoys.Print.*;

/**
 *
 * @author Konstantin Kovalenko
 */
public class HtmlScanner {

    private URL url;
    private boolean URLisValid = true;
    private HttpURLConnection conn;
    private BufferedReader in;
    private String webPageURL;
    private String pageCodeString;
    private static Map<String, Set<? extends String>> outLinks = new HashMap<>();
    private static Map<Integer, Set<? extends String>> innerLinks = new HashMap<>();
    private Set<String> tempInnerLinks = new TreeSet<>();
    private Set<String> tempOuterLinks = new TreeSet<>();
    private Set<String> haveToVisit = new TreeSet();
    private static String visitedInnerLinks = "";
    private static PriorityQueue<String> pq = new PriorityQueue<>();
    private SaveInnerData sid = new SaveInnerData();
    private SaveOuterData sod = new SaveOuterData();

    public void scanWebPage(String webPageUrl) {
        try {
            webPageURL = webPageUrl;
            url = new URL(webPageURL);
            visitedInnerLinks += webPageURL + "\n";
        } catch (MalformedURLException e) {
            System.err.println(e);
            URLisValid = false;
        }
        if (URLisValid) {
            try {
                conn = (HttpURLConnection) url.openConnection();
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                final String PHTML = ".html.";
                while ((pageCodeString = in.readLine()) != null) {
                    if (checkPCSOut(pageCodeString)) {
                        sod.saveToTempOuterLinks(pageCodeString);
                    }
                    while (checkPCSIn(pageCodeString)) {
                        sid.saveToTempInnerLinks(pageCodeString);
                        pageCodeString = pageCodeString.substring(pageCodeString.indexOf(".html") + PHTML.length());
                    }
                }
                if (!tempInnerLinks.isEmpty()) {
                    fillPriorityQueue();
                    sid.saveToInnerLinksMap();
                }
                if (!tempOuterLinks.isEmpty()) {
                    sod.saveToOutLinksMap();
                }
                tempOuterLinks = new TreeSet<>();
                while (pq.peek() != null) {
                    scanWebPage(pq.remove());
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Method returns matches in the incoming String by pattern
     *
     * @param stringToCheck is a String to be checked
     * @param pattern is a String pattern
     */
    private boolean checkPageCodeString(String stringToCheck, String pattern) {
        Pattern myPattern = Pattern.compile(pattern);
        Matcher myMatcher = myPattern.matcher(stringToCheck);
        return myMatcher.matches();
    }

    private boolean checkPCSIn(String stringToCheck) {
        return (checkPageCodeString(stringToCheck, ".*href=\".*" + returnCleanURL(webPageURL) + ".*\\.html.*")
                || checkPageCodeString(stringToCheck, ".*href=\"\\w*\\.html.*"))
                && !checkPageCodeString(stringToCheck, ".*href=\".*\\.xml.*");
    }

    private boolean checkPCSOut(String stringToCheck) {
        return checkPageCodeString(stringToCheck, ".*href=\"http.*")
                && !checkPageCodeString(stringToCheck, ".*" + returnCleanURL(webPageURL) + ".*");
    }

    private String returnCleanURL(String inputString) {
        String newString = inputString;
        if (URLisValid) {
            final String HTTP = "http://";
            final String HTTPS = "https://";
            final int indOfHttp = newString.indexOf("http");
            final int indOfHttps = newString.indexOf("https");
            final int indOfSlash = newString.indexOf("/", (indOfHttp + HTTP.length()));
            final int indOfSlashHttps = newString.indexOf("/", (indOfHttp + HTTPS.length()));
            if (indOfSlashHttps != -1) {
                if (indOfHttps != -1) {
                    newString = newString.substring(indOfHttps + HTTPS.length(), indOfSlashHttps);
                    return newString;
                } else {
                    newString = newString.substring(indOfHttp + HTTP.length(), indOfSlash);
                    return newString;
                }
            } else {
                if (indOfHttps != -1) {
                    newString = newString.substring(indOfHttps + HTTPS.length());
                    return newString;
                } else {
                    newString = newString.substring(indOfHttp + HTTP.length());
                    return newString;
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void fillPriorityQueue() {
        haveToVisit = new TreeSet();
        for (String s : tempInnerLinks) {
            if (!visitedInnerLinks.contains(s)) {
                haveToVisit.add(s);
            }
        }
        if (!haveToVisit.isEmpty()) {
            pq = new PriorityQueue<>(haveToVisit);
        }
    }

    private class SaveInnerData {

        private Set<String> wpurlSet = new TreeSet<>();

        synchronized void saveToTempInnerLinks(String inputString) {
            String workString = inputString;
            if (checkPageCodeString(workString, ".*href=\".*" + returnCleanURL(webPageURL) + ".*\\.html.*")) {
                final String HREF = "href=:";
                final int indOfHrefHttp = workString.indexOf("href=\"http");
                final int indOfSlash = workString.indexOf("\"", (indOfHrefHttp + HREF.length()));
                if (indOfSlash != -1) {
                    workString = workString.substring((indOfHrefHttp + HREF.length()), indOfSlash);
                    tempInnerLinks.add(workString);
                }
            }
            if (checkPageCodeString(workString, ".*href=\"\\w*\\.html.*")) {
                final String HREF = "href=:";
                final int indOfHref = workString.indexOf("href=\"");
                final int indOfSlash = workString.indexOf("\"", (indOfHref + HREF.length()));
                if (indOfSlash != -1) {
                    workString = workString.substring((indOfHref + HREF.length()), indOfSlash);
                    if (webPageURL.equals("http://" + returnCleanURL(webPageURL))) {
                        tempInnerLinks.add("http://" + returnCleanURL(webPageURL) + "/" + workString);
                    } else if (webPageURL.equals("http://" + returnCleanURL(webPageURL) + "/")) {
                        tempInnerLinks.add(webPageURL + workString);
                    } else {
                        String workString2 = webPageURL;
                        final int lastIndOfSlash = workString2.lastIndexOf("/");
                        workString2 = workString2.substring(0, lastIndOfSlash + 1);
                        tempInnerLinks.add(workString2 + workString);
                    }
                }
            }
        }

        synchronized void saveToInnerLinksMap() {
            if (innerLinks.isEmpty()) {
                wpurlSet.add(webPageURL);
                innerLinks.put(0, wpurlSet);
                innerLinks.put(1, tempInnerLinks);
            } else {
                String TIL = "";
                String workString = "";
                wpurlSet = new TreeSet<>();
                for (String s : tempInnerLinks) {
                    TIL += s + "\n";
                }
                for (int i : innerLinks.keySet()) {
                    for (String s : innerLinks.get(i)) {
                        if (!TIL.contains(s)) {
                            workString += s + "\n";
                        }
                    }
                }
                for (String s : workString.split("\n")) {
                    wpurlSet.add(s);
                }
                for (Integer i : innerLinks.keySet()) {
                    for (String s : innerLinks.get(i)) {
                        if (webPageURL.equals(s)) {
                            if (innerLinks.containsKey(i + 1)) {
                                for (String s2 : innerLinks.get(i + 1)) {
                                    wpurlSet.add(s2);
                                }
                                innerLinks.put(i + 1, wpurlSet);
                            } else {
                                innerLinks.put(i + 1, wpurlSet);
                            }
                        }
                    }
                }
            }
        }
    }

    private class SaveOuterData {

        synchronized void saveToTempOuterLinks(String inputString) {
            String workString = inputString;
            final String HREF = "href=:";
            final int indOfHrefHttp = workString.indexOf("href=\"http");
            final int indOfSlash = workString.indexOf("\"", (indOfHrefHttp + HREF.length()));
            if (indOfSlash != -1) {
                workString = workString.substring((indOfHrefHttp + HREF.length()), indOfSlash);
                tempOuterLinks.add(workString);
            }
        }

        synchronized void saveToOutLinksMap() {
            outLinks.put(webPageURL, tempOuterLinks);
        }
    }

    public static void main(String[] args) {
        HtmlScanner hs = new HtmlScanner();
        //hs.scanWebPage("http://manga-online.com.ua/manga/shingeki-no-kyojin/page,45,25875-Shingeki-no-Kyojin.-Tom-16.-Glava-065.-Mechti-i-proklyatya.html");
        //hs.scanWebPage("http://www.lostfilm.tv");
        hs.scanWebPage("http://www.beluys.com/html_basics/html_page.html");
        //hs.scanWebPage("http://www.smartincom.ru/html/");
        println("InnerLinks: ");
        for (int i : innerLinks.keySet()) {
            println("\tClicks : " + i);
            for (String s : innerLinks.get(i)) {
                println("\t\t" + s);
            }
        }
        println();
        println("OutLinks: ");
        for (String key : outLinks.keySet()) {
            println("\tPage : " + key);
            for (String value : outLinks.get(key)) {
                println("\t\t" + value);
            }
        }
    }

}
