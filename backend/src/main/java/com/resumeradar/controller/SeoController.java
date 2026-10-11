package com.resumeradar.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeoController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "ResumeRadar — Free On-Device ATS Resume Checker & Scanner");
        model.addAttribute("metaDescription", "Scan your resume against job descriptions on your device without uploading files to third-party servers. Get instant ATS compatibility feedback, score trends, and bullet rewrites.");
        return "index";
    }

    @GetMapping("/guides/ats-friendly-formatting")
    public String atsFormattingGuide(Model model) {
        model.addAttribute("pageTitle", "Complete Guide to ATS-Friendly Resume Formatting | ResumeRadar");
        model.addAttribute("metaDescription", "Learn how applicant tracking systems parse your resume. Avoid table pitfalls, graphic headers, multi-column layouts, and font errors to pass automated screenings.");
        return "guides/ats-formatting";
    }

    @GetMapping("/guides/resume-writing-tips")
    public String resumeWritingTips(Model model) {
        model.addAttribute("pageTitle", "Proven Tech Resume Writing Tips & Power Verbs | ResumeRadar");
        model.addAttribute("metaDescription", "Transform weak resume bullets into high-impact accomplishments using the Google XYZ formula and quantifiable metrics for software engineering roles.");
        return "guides/resume-tips";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url>
                    <loc>https://resumeradar.com/</loc>
                    <changefreq>daily</changefreq>
                    <priority>1.0</priority>
                  </url>
                  <url>
                    <loc>https://resumeradar.com/guides/ats-friendly-formatting</loc>
                    <changefreq>weekly</changefreq>
                    <priority>0.8</priority>
                  </url>
                  <url>
                    <loc>https://resumeradar.com/guides/resume-writing-tips</loc>
                    <changefreq>weekly</changefreq>
                    <priority>0.8</priority>
                  </url>
                  <url>
                    <loc>https://resumeradar.com/login</loc>
                    <changefreq>monthly</changefreq>
                    <priority>0.5</priority>
                  </url>
                  <url>
                    <loc>https://resumeradar.com/register</loc>
                    <changefreq>monthly</changefreq>
                    <priority>0.5</priority>
                  </url>
                </urlset>
                """.trim();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robotsTxt() {
        return """
                User-agent: *
                Allow: /
                Allow: /guides/
                Allow: /login
                Allow: /register
                Disallow: /dashboard
                Disallow: /resumes/
                Disallow: /jobs/
                Disallow: /mentor/
                Disallow: /admin/
                Disallow: /api/

                Sitemap: https://resumeradar.com/sitemap.xml
                """.trim();
    }
}
