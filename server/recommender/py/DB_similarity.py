import pandas as pd
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import sqlite3
import os.path
import errno
from datetime import date, timedelta, datetime
import datetime
import sys
from sklearn.metrics.pairwise import cosine_similarity


def main(ID):
    print(ID)
    print(1)
    start = datetime.datetime.strptime('2020-09-05', '%Y-%m-%d')  # datetime.date(start_day) #탐색 시작 날짜, 끝 날짜
    end = datetime.datetime.strptime('2020-09-05', '%Y-%m-%d')  # datetime.date(datetime.now())

    Save = pd.DataFrame()
    while start <= end:
        DB = pd.read_csv("./articleDB/2020-09-05_DB.csv") #DB파일 데이터프레임 생성
        try:
            User = pd.read_csv('./recommender/userprofile/'+ID+'/UserData.csv') #User파일 데이터프레임 생성
        except:
            User = pd.DataFrame() #존재하지 않을 경우 빈 데이터 프레임 생성
        DB_sum = DB["본문"]
        DB = DB.drop(["aid", "oid", "sid1", "sid2", "본문"], axis=1) #필요하지 않은 컬럼 삭제

        compare_DB = DB.iloc[[0,1]] #키워드 및 가중치 데이터만 추출
        compare_DB.drop([compare_DB.index[1]], inplace=True)
        User = pd.concat([User, compare_DB], axis = 0)
        User.reset_index(drop=True, inplace=True)
        try:
            User.drop([User.index[1]], inplace=True)
            User.fillna(1/User.shape[1], inplace=True)
        except:
            User.iloc[:,:] = 1/User.shape[1]
        User.sort_index(axis=1, inplace=True) #키워드 오름 차순 정렬

        DB = pd.concat([DB, User], axis=0) #데이터프레임 비교를 위해 합침
        DB.reset_index(drop=True, inplace=True)
        DB.drop([DB.index[-1]], inplace=True)
        DB.fillna(0, inplace=True)
        DB.sort_index(axis=1, inplace=True) #키워드 오름차순 정렬

        User.insert(0, '본문', 'Nan')
        DB.insert(0, '본문', DB_sum)

        DB = pd.concat([User, DB], ignore_index=True)

        DB_sum = DB["본문"]
        DB_compare = DB.drop(["본문"], axis=1)
        DB_compare = DB_compare.replace([np.inf, -np.inf], np.nan)
        DB_compare = DB_compare.fillna(0)
        #nan값 및 불필요한 값 삭제

        DB_cosine = pd.DataFrame(cosine_similarity(DB_compare)[0][0:]) #유사도 계산 : 코사인 유사도
        #DB_pearson = DB_compare.corr(method='pearson')[0][0:] #유사도 계산 : 피어슨 유사도

        DB_NEW = pd.DataFrame()
        DB_NEW = pd.concat([DB_sum, DB_cosine], axis=1) #계산된 유사도 추가 : 코사인 유사도
        #DB_NEW = pd.concat([DB_sum, DB_pearson], axis=1) #계산된 유사도 추가 : 피어슨 유사도
        DB_NEW.columns = ['link', 'similarity']
        DB_sort = DB_NEW.sort_values(by=['similarity'], axis=0, ascending=False)
        DB_sort.drop([DB_sort.index[0]], inplace=True)
        Save = pd.concat([Save, DB_sort], ignore_index=True)
        start += timedelta(1)

    Save = Save.sort_values(by=['similarity'], axis=0, ascending=False)
    Save = Save.head(100) #상위 100개 추천 기사 목록 데이터프레임
    con = sqlite3.connect('./recommender/userprofile/'+ID+'/recommanddb') #sqlite 데이터 베이스 생성
    Save.to_sql('tblink', con, if_exists='replace')
    print('finish db_similarity')


if __name__ == "__main__":
    main(sys.argv[1])
