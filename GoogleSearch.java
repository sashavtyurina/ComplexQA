// package com.journaldev.jsoup;
 
import java.io.IOException;
import java.util.Scanner;
import org.jsoup.*; 
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
 
public class GoogleSearch {
 
    private static final String GOOGLE_SEARCH_URL = "https://www.google.com/search";
    public static HashMap<String, String> searchTerms(String searchQuery, int num) throws IOException {
        // Taking search term input from console
        // Scanner scanner = new Scanner(System.in);
        // System.out.println("Please enter the search term.");
        // String searchTerm = scanner.nextLine();
        // System.out.println("Please enter the number of results. Example: 5 10 20");
        // int num = scanner.nextInt();
        // scanner.close();
         
        String searchURL = GOOGLE_SEARCH_URL + "?q="+searchQuery+"&num="+num;
        //without proper User-Agent, we will get 403 error
        Document doc = Jsoup.connect(searchURL).userAgent("Mozilla/5.0").get();
        HashMap<String, String> resultDocs = new HashMap<String, String>();
         
        //below will print HTML data, save it to a file and open in browser to compare
        //System.out.println(doc.html());
         
        //If google search results HTML change the <h3 class="r" to <h3 class="r1"
        //we need to change below accordingly
        Elements results = doc.select("h3.r > a");
        // Vector<String> urls = new Vector<String>();
        // Vector<String> docs = new Vector<String>();
        int counter = 0;
        for (Element result : results) {
            String linkHref = result.attr("href");
            // System.out.println(counter + ":::" + linkHref);
            counter ++;
            linkHref = linkHref.substring(7, linkHref.indexOf("&"));
            // System.out.println("Fetching URL:::" + linkHref);
            try {

            // urls.add(linkHref);

                Document htmlDoc = Jsoup.connect(linkHref).userAgent("Mozilla/5.0").get();
            // docs.add(htmlDoc);

                resultDocs.put(linkHref, htmlDoc.toString());
            } catch (HttpStatusException e) {
                System.err.println( e.getClass().getName() + ": " + e.getMessage() );
                continue;
            } catch (Exception e) {
                System.err.println( e.getClass().getName() + ": " + e.getMessage() );
                continue;
            }
            // System.out.println(htmlDoc.toString());

            // System.out.println(linkHref);
            // System.out.println(linkHref.substring(7, linkHref.indexOf("&")));
            
            // String linkText = result.text();
            // System.out.println("Text::" + linkText + ", URL::" + linkHref.substring(6, linkHref.indexOf("&")));
        }
        return resultDocs;
    }
 
}