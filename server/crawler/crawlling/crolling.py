from bs4 import BeautifulSoup
import re
import urllib.request
import csv
import os
import pandas as pd
import errno
import requests

from newspaper import Article
from gensim.summarization.summarizer import summarize
from gensim.summarization import keywords
from konlpy.tag import Kkma
from datetime import date, timedelta, datetime
import datetime


class GetInformation:  # 기사 링크 목록, 기사, 제목 등 정보 추출 클래스
    def __init__(self, sid1, sid2, dateinfo):
        self.publisher = {}  # 출판사 목록이 저장될 곳
        self.articleInfo_DataFrame = pd.DataFrame()  # 기사에 대한 정보 dict형으로 저장
        self.Link = 'https://news.naver.com/main/list.nhn?mode=LS2D&mid=shm&sid2=' + sid2 + '&sid1=' + sid1 + '&date=' + dateinfo + '&page='
        self.GetSid2(self.Link, sid1, sid2)

    def Cleaning(self, text):  # text를 받아 쓸데없는 문장부호 등을 제거
        return text

    def GetSid2(self, URL, sid1, sid2):  # 소분류 탐색, 소분류마다 최대 페이지 탐색, 전체 페이지 링크 저장 함수
        pageLimit, current_page_num, X = 1, 1, 0

        while (int(pageLimit) >= current_page_num):
            url_with_page_num = URL + str(current_page_num)  # 기본 URL과 page_num 합침
            session = requests.Session()
            headers = {
                "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit 537.36 (KHTML, like Gecko) Chrome",
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            }
            source_url = session.get(url_with_page_num, headers=headers).content
            # source_url = urllib.request.urlopen(url_with_page_num)
            soup = BeautifulSoup(source_url, 'lxml', from_encoding='utf-8')  # 크롤링 패키지 beautifulsoup로 링크 열기

            for title in soup.find_all('dt', {'class': 'photo'}):  # 페이지에서 링크 추출하기 위해서 html에 dt tag의 photo class이용
                articleInfo = {}
                title_t = title.find('a')["href"]
                article = Article(str(title_t), language='ko')
                article.download()
                article.parse()
                articleText = article.text

                articleInfo.update(zip(['aid', 'oid', 'sid1', 'sid2', '본문', '제목'],
                                       [*self.GetSidOidAid(title_t), sid1, sid2, title_t, article.title]))

                try:
                    articleInfo["요약"] = summarize(articleText)  # gensim 패키지 이용해 요약
                except ValueError:
                    articleInfo["요약"] = ''
                    pass

                for n, keyWord in enumerate(
                        keywords(articleText, words=5, scores=True)):  # gensim 패키지 이용해 keyword추출\n",
                    articleInfo[str("키워드 " + str(n))] = keyWord[0]  # 키워드의 숫자 자릿수를 2자리로 고정하여 키값생성
                    articleInfo[str("키워드 " + str(n) + " 가중치")] = keyWord[1]

                Info = pd.DataFrame(articleInfo, index=[X])  # articleInfo를 하나의 row로 하여 DataFrame 생성
                self.articleInfo_DataFrame = self.articleInfo_DataFrame.append(Info)  # 하나의 row를 기존의 DataFrame 마지막으로 추가
                X += 1  # 기사 인덱스 번호 증가

            if (soup.find('a', {'class': 'next nclicks(fls.page)'}) == None):
                pageLimit = ' '.join(soup.find('div', {'class': 'paging'}).text.split('\n')).split()[-1]

            pageLimit = str(int(pageLimit) + 1)
            current_page_num += 1

    def GetSidOidAid(self, url):  # 기사의 신문사코드 기사코드 추출
        oid = re.findall('oid=[0-9]+', url)[0][4:]
        aid = re.findall('aid=[0-9]+', url)[0][4:]

        return aid, oid


def main():
    categorySid1 = {'100': ['264', '265', '268', '266', '267', '269'],
                    '101': ['259', '258', '261', '771', '260', '262', '310', '263'],
                    '102': ['249', '250', '251', '254', '252', '59', '255', '256', '276', '257'],
                    '103': ['241', '239', '240', '237', '238', '376', '242', '243', '244', '248', '245'],
                    '104': ['231', '232', '233', '234', '322'],
                    '105': ['731', '226', '227', '230', '732', '283', '229', '228']}
    publisherOid = {}  # 신문사 코드, 이름 계속 누적해서 저장될 곳
    start = datetime.datetime.strptime('2020-09-05', '%Y-%m-%d')  # datetime.date(start_day) #탐색 시작 날짜, 끝 날짜
    end = datetime.datetime.strptime('2020-09-05', '%Y-%m-%d')  # datetime.date(datetime.now())

    # 'https://news.naver.com/main/list.nhn?mode=LS2D&mid=shm&sid2='+sid2+'&sid1='+sid1+'&date='+start.strftime('%Y%m%d')+'&page=' URL양식

    while start <= end:  # 시작날짜 부터 끝날짜까지
        for sid1 in categorySid1:  # 대분류부터 기사 크롤링'
            articleInfo_dir = "./articleInfo/" + start.strftime('%Y-%m-%d') + "/" + sid1 + "/"  # 파일 경로
            try:  # 파일 경로 생성, 경로가 존재 하지 않을 경우 파일 경로 생성
                if not (os.path.isdir(articleInfo_dir)):
                    os.makedirs(os.path.join(articleInfo_dir))
            except OSError as e:
                if e.errno != errno.EEXIST:
                    print("Dir error")
                raise

            for sid2 in categorySid1[sid1]:  # 소분류들 차례로 탐색 #그 페이지의 모든 기사링크 모음
                content = GetInformation(sid1, sid2, start.strftime('%Y%m%d'))  # 대분류 링크로부터 소분류 추츌
                articleInfo_DataFrame = content.articleInfo_DataFrame
                articleInfo_filename = "sid2_" + sid2 + ".csv"  # 파일 이름
                articleInfo_DataFrame.to_csv(
                    articleInfo_dir + articleInfo_filename)  # 지정한 경로, 파일이름으로 DataFrame 저장
                publisherOid.update(content.publisher)

        start += timedelta(1)  # 날짜가 끝나면 날짜 더해서 한번 더


if __name__ == "__main__":
    main()