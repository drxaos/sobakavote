package com.github.drxaos.sobakavote

import groovy.json.JsonSlurper;

public class Main {

    public static void main(String[] args) {

        def id = 76794
        def pageid = 953
        def sleep = 374
        def minRec = 3
        def minLetters = 5

        def page = [
                "curl", "-vvv", "http://top50.chlb.sobaka.ru/vote/music/${id}",
                "-H", "Connection: keep-alive",
                "-H", "Pragma: no-cache",
                "-H", "Cache-Control: no-cache",
                "-H", "Upgrade-Insecure-Requests: 1",
                "-H", "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36",
                "-H", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
                "-H", "DNT: 1",
                "-H", "Referer: http://away.vk.com/away.php",
                "-H", "Accept-Encoding: gzip, deflate",
                "-H", "Accept-Language: en-US,en;q=0.9,ru;q=0.8",
                "-H", "Cookie: _ga=GA1.2.1417632665.1534438789; _ym_uid=1534438789809973970; _ym_d=1534438789; _gid=GA1.2.1877697995.1534700896; _ym_isad=1; 2619cc29e69ad5c34ec35805f2db9d9b0b05e6fb=${id}; 5aa8ad6c0e8ffb611d5be57458ff5245e73d07e1=76928; _gat=1; _ym_visorc_38726385=w; __atuvc=2%7C33%2C20%7C34; __atuvs=5b79c606d7b4372b000; __atssc=vk%3B4",
                "--compressed"
        ].execute()
        def outBuf = new StringBuffer()
        def errBuf = new StringBuffer()
        page.consumeProcessOutput(outBuf, errBuf)
        page.waitForOrKill(10000)
        def pageHeaders = errBuf.toString()
        def phpsessid = (pageHeaders.readLines()
                .find { it.startsWith("< Set-Cookie: PHPSESSID=") }
                =~ /PHPSESSID=([a-z0-9]+)/)[0][1]
        println "PHPSESSID: " + phpsessid

        Thread.sleep(sleep)
        Thread.sleep(sleep)

        def popup = [
                "curl", "http://top50.chlb.sobaka.ru/post/addVote/postId/${id}/pageId/${pageid}",
                "-X", "POST",
                "-H", "Pragma: no-cache",
                "-H", "Origin: http://top50.chlb.sobaka.ru",
                "-H", "Accept-Encoding: gzip, deflate",
                "-H", "Accept-Language: en-US,en;q=0.9,ru;q=0.8",
                "-H", "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36", "-H", "Accept: application/json, text/javascript, */*; q=0.01",
                "-H", "Cache-Control: no-cache",
                "-H", "X-Requested-With: XMLHttpRequest",
                "-H", "Cookie: PHPSESSID=${phpsessid}; _ga=GA1.2.1616476502.1534705497; _gid=GA1.2.57435428.1534705497; _ym_uid=1534705497927362427; _ym_d=1534705497; _ym_isad=2; _gat=1; _ym_visorc_38726385=w; __atuvc=2%7C34; __atuvs=5b79c6a1c927f0ed000",
                "-H", "Connection: keep-alive",
                "-H", "Referer: http://top50.chlb.sobaka.ru/vote/music/${id}",
                "-H", "Content-Length: 0",
                "-H", "DNT: 1", "--compressed"
        ].execute()
        outBuf = new StringBuffer()
        errBuf = new StringBuffer()
        popup.consumeProcessOutput(outBuf, errBuf)
        popup.waitForOrKill(10000)
        def popupText = outBuf.toString()
        def capHash = (popupText =~ /\\\/post\\\/captcha\\\/v\\\/([a-z0-9]+)/)[0][1]
        println "HASH: " + capHash

        Thread.sleep(sleep)

        def recognized = null
        def recMap = [:]

        while (!recognized) {
            def captcha = [
                    "curl", "http://top50.chlb.sobaka.ru/post/captcha/v/${capHash}",
                    "-H", "Pragma: no-cache",
                    "-H", "DNT: 1",
                    "-H", "Accept-Encoding: gzip, deflate",
                    "-H", "Accept-Language: en-US,en;q=0.9,ru;q=0.8",
                    "-H", "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36",
                    "-H", "Accept: image/webp,image/apng,image/*,*/*;q=0.8",
                    "-H", "Referer: http://top50.chlb.sobaka.ru/vote/music/${id}",
                    "-H", "Cookie: PHPSESSID=${phpsessid}; _ga=GA1.2.1616476502.1534705497; _gid=GA1.2.57435428.1534705497; _ym_uid=1534705497927362427; _ym_d=1534705497; _ym_isad=2; _gat=1; _ym_visorc_38726385=w; __atuvc=2%7C34; __atuvs=5b79c6a1c927f0ed000",
                    "-H", "Connection: keep-alive",
                    "-H", "Cache-Control: no-cache",
                    "--compressed",
                    "--output", "input.png"
            ].execute()
            outBuf = new StringBuffer()
            errBuf = new StringBuffer()
            captcha.consumeProcessOutput(outBuf, errBuf)
            captcha.waitForOrKill(10000)

            def tess = "tesseract -c tessedit_char_whitelist=abcdefghijklmnopqrstuvwxyz input.png stdout".execute()
            tess.waitForOrKill(20000)
            def text = tess.inputStream.text.replaceAll(/[^a-z]/, "")

            if (text.length() < minLetters) {
                println "TEXT: " + text + " (skip)"
                continue
            }

            def rec = recMap[text] = (recMap[text] ?: 0) + 1
            println "TEXT: " + text + " (" + rec + ")"
            recognized = rec >= minRec ? text : null

            Thread.sleep(sleep)
        }

        def result = [
                "curl", "http://top50.chlb.sobaka.ru/post/addVote/postId/${id}/pageId/${pageid}",
                "-H", "Pragma: no-cache",
                "-H", "Origin: http://top50.chlb.sobaka.ru",
                "-H", "Accept-Encoding: gzip, deflate",
                "-H", "Accept-Language: en-US,en;q=0.9,ru;q=0.8",
                "-H", "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36",
                "-H", "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
                "-H", "Accept: application/json, text/javascript, */*; q=0.01",
                "-H", "Cache-Control: no-cache",
                "-H", "X-Requested-With: XMLHttpRequest",
                "-H", "Cookie: PHPSESSID=${phpsessid}; _ga=GA1.2.1616476502.1534705497; _gid=GA1.2.57435428.1534705497; _ym_uid=1534705497927362427; _ym_d=1534705497; _ym_isad=2; _ym_visorc_38726385=w; __atuvc=3%7C34; __atuvs=5b79c6a1c927f0ed001",
                "-H", "Connection: keep-alive",
                "-H", "Referer: http://top50.chlb.sobaka.ru/vote/music/${id}",
                "-H", "DNT: 1",
                "--data", "CaptchaModel%5BverifyCode%5D=${recognized}",
                "--compressed"
        ].execute()
        outBuf = new StringBuffer()
        errBuf = new StringBuffer()
        result.consumeProcessOutput(outBuf, errBuf)
        result.waitForOrKill(10000)
        def success = outBuf.toString().startsWith("{\"ratings\"")
        println "success: " + success

        if (success) {
            def ratings = new JsonSlurper().parseText(outBuf.toString()).ratings
            println "current: " + ratings["" + id]
            def max = ratings.values().max()
            println "first: " + max
            println "second: " + ratings.values().findAll { it != max }.max()
        }
    }

}
