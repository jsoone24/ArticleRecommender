from __future__ import absolute_import, unicode_literals
from celery import shared_task

from django_project.celery import app
from .py import User_update, DB_similarity
import os
from shutil import copy
import time

@shared_task #define tasks runs in celery. this functions run asyncronically
def Run_User_Update(ID): 
    print("inside celery", ID)
    dirpath = './recommender/userprofile/'+ID 

    if not (os.path.isdir(dirpath)):
        os.mkdir(dirpath)

    print("running User_Update...")
    User_update.main(ID) #run User_Update. which updates keyword weights in userfile
    print("running DB_similarilty...")
    Run_DB_similarity(ID) #list and sort articles comparing article keyword weights and user keyword weights
    print("finished")

def Run_DB_similarity(ID):
    DB_similarity.main(ID)