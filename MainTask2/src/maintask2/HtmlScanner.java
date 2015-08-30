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
public class HtmlScanner{
    private URL url;
    private boolean URLisValid = true;
    private HttpURLConnection conn;
    private BufferedReader in;
    private String webPageURL;
    private String pageCodeString;
    public static Map<String,Set<? extends String>> outLinks = new LinkedHashMap<>();
    public static Map<Integer,Set<? extends String>> innerLinks = new LinkedHashMap<>();
    private Set<String> tempInnerLinks = new TreeSet<>();
    private Set<String> tempOuterLinks = new TreeSet<>();
    private Set<String> haveToVisit = new TreeSet();
    private static String visitedInnerLinks = "";
    private static PriorityQueue<String> pq = new PriorityQueue<>();
    SaveInnerData sid = new SaveInnerData();
    SaveOuterData sod = new SaveOuterData();

    public void scanWebPage(String webPageUrl){
        try{
            webPageURL = webPageUrl;
            url = new URL(webPageURL);
            visitedInnerLinks+=webPageURL+"\n";
        }catch(MalformedURLException e){
            System.err.println(e);
            URLisValid = false;
        }
        if(URLisValid){
            try{
                conn = (HttpURLConnection) url.openConnection();
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while((pageCodeString=in.readLine())!=null){
                    if(checkPageCodeString(pageCodeString,".*href=\"http.*")&&!checkPageCodeString(pageCodeString,".*"+returnCleanURL()+".*"))
                        sod.saveToTempOuterLinks(pageCodeString);
                    while((checkPageCodeString(pageCodeString,".*href=\".*"+returnCleanURL()+".*\\.html.*")||checkPageCodeString(pageCodeString,".*href=\"\\w*\\.html.*"))&&
                            !checkPageCodeString(pageCodeString,".*href=\".*\\.xml.*")){
                        sid.saveToTempInnerLinks(pageCodeString);
                        pageCodeString = pageCodeString.substring(pageCodeString.indexOf(".html")+6);
                    }
                }
                if(!tempInnerLinks.isEmpty()){
                    fillPriorityQueue();
                    sid.saveToInnerLinksMap();
                }
                if(!tempOuterLinks.isEmpty())
                    sod.saveToOutLinksMap();
                tempOuterLinks = new TreeSet<>();
                while(pq.peek()!=null){
                    scanWebPage(pq.remove());
                }
            }catch(IOException e){
                System.err.println(e);
            }
        }
    }
    /**
    * Method returns matches in the incoming String by pattern
    * @param stringToCheck is a String to be checked
    * @param pattern is a String pattern
    */
    private boolean checkPageCodeString(String stringToCheck, String pattern) {
        Pattern myPattern = Pattern.compile(pattern);
        Matcher myMatcher = myPattern.matcher(stringToCheck);
        return myMatcher.matches();
    }
    /**
     * @return main page URL as a String
     */
    private String returnCleanURL() {
        String newString = webPageURL;
        if (URLisValid) {
            int a = newString.indexOf("http");
            int a2 = newString.indexOf("https");
            int b = newString.indexOf("/", (a + 7));
            int b2 = newString.indexOf("/", (a + 8));
            if (b2 != -1) {
                if(a2!=-1){
                    newString = newString.substring(a2+8, b2);
                    return newString;
                }else{
                    newString = newString.substring(a+7, b);
                    return newString;
                }
            }else{
                if(a2!=-1){
                    newString = newString.substring(a2+8);
                    return newString;
                }else{
                    newString = newString.substring(a+7);
                    return newString;
                }
            }
        } else {
            return "";
        }
    }
    private void fillPriorityQueue(){
        haveToVisit = new TreeSet();
        for(String s : tempInnerLinks){
            if(!visitedInnerLinks.contains(s))
                haveToVisit.add(s);
        }
        if(!haveToVisit.isEmpty())
            pq = new PriorityQueue<>(haveToVisit);
    }
    private class SaveInnerData{
        private Set<String> wpurlSet = new TreeSet<>();
        private String TIL;
        private String workString;
        synchronized void saveToTempInnerLinks(String pcs){
            String workString = pcs;
            if(checkPageCodeString(workString,".*href=\".*"+returnCleanURL()+".*\\.html.*")){
                int a = workString.indexOf("href=\"http");
                int b = workString.indexOf("\"",(a+6));
                if(b!=-1){
                    workString = workString.substring((a+6), b);
                    tempInnerLinks.add(workString);
                }
            }
            if(checkPageCodeString(workString,".*href=\"\\w*\\.html.*")){
                int a = workString.indexOf("href=\"");
                int b = workString.indexOf("\"",(a+6));
                if(b!=-1){
                    workString = workString.substring((a+6), b);
                    if(webPageURL.equals("http://"+returnCleanURL())){
                        tempInnerLinks.add("http://"+returnCleanURL()+"/"+workString);
                    }else if(webPageURL.equals("http://"+returnCleanURL()+"/")){
                        tempInnerLinks.add(webPageURL+workString);
                    }else{
                        String workString2 = webPageURL;
                        int c = workString2.lastIndexOf("/");
                        workString2 = workString2.substring(0, c+1);
                        tempInnerLinks.add(workString2+workString);
                    }
                    
                }
            }
        }
        synchronized void saveToInnerLinksMap(){
            if(innerLinks.isEmpty()){
                wpurlSet.add(webPageURL);
                innerLinks.put(0, wpurlSet);
                innerLinks.put(1, tempInnerLinks);
            }else{
                TIL = "";
                workString = "";
                wpurlSet = new TreeSet<>();
                for(String s : tempInnerLinks)
                    TIL +=s+"\n";
                for(int i : innerLinks.keySet()){
                    for(String s : innerLinks.get(i)){
                        if(!TIL.contains(s))
                            workString += s+"\n";
                    }
                }
                for(String s : workString.split("\n"))
                    wpurlSet.add(s);
                for(Integer i : innerLinks.keySet()){
                    for(String s : innerLinks.get(i)){
                        if(webPageURL.equals(s)){
                            if(innerLinks.containsKey(i+1)){
                                for(String s2 : innerLinks.get(i+1))
                                    wpurlSet.add(s2);
                                innerLinks.put(i+1, wpurlSet);
                            }else
                                innerLinks.put(i+1, wpurlSet);
                        }
                    }
                }
            }
        }
    }
    private class SaveOuterData{
        synchronized void saveToTempOuterLinks(String pcs){
            String workString = pcs;
            int a = workString.indexOf("href=\"http");
            int b = workString.indexOf("\"",(a+6));
            if(b!=-1){
                workString = workString.substring((a+6), b);
                tempOuterLinks.add(workString);
            }
        }
        synchronized void saveToOutLinksMap(){
            outLinks.put(webPageURL, tempOuterLinks);
        }
    }
    

    
    public static void main(String[] args) throws Exception{
        HtmlScanner hs = new HtmlScanner();
        hs.scanWebPage("http://www.beluys.com");
        //hs.scanWebPage("http://manga-online.com.ua/manga/shingeki-no-kyojin/page,45,25875-Shingeki-no-Kyojin.-Tom-16.-Glava-065.-Mechti-i-proklyatya.html");
        //hs.scanWebPage("http://www.lostfilm.tv");
        //hs.scanWebPage("http://www.beluys.com/html_basics/html_page.html");
        //hs.scanWebPage("http://www.smartincom.ru/html/");

       /*println("VisitedInnerLinks: ");
       String[] vil = visitedInnerLinks.split("\n");
       for(String s : vil)
           println("    "+s);
       println();*/
       println("InnerLinks: ");
       for(int i : innerLinks.keySet()){
           println("    Clicks : "+i);
           for(String s : innerLinks.get(i))
               println("        "+s);
       }
       println();
       println("OutLinks: ");
       for(String key : outLinks.keySet()){
           println("    Page : "+key);
           for(String value : outLinks.get(key))
               println("            "+value);
       }
    }
    
}
