from django.shortcuts import render
from django.http import FileResponse
from django.http import HttpResponse
from django.http import HttpResponseRedirect
from django.shortcuts import render
import os
import mimetypes
from shutil import copy

from .tasks import Run_User_Update

# Create your views here.
from .forms import UploadFileForm
from django.conf import settings

def SendUserFile(request, ID): #When android device access through exact link, there's android unique id
    dirpath = './recommender/userprofile/'+ID
    filepath = './recommender/userprofile/'+ID+'/recommenddb'

    if not (os.path.isdir(dirpath)): #if directory for id is not defined, then make new directory.
        os.mkdir(dirpath)

    if not (os.path.isfile(filepath)): #if new user, then copy default recommend file.
        copy('./recommender/userprofile/default/recommenddb',dirpath)

    response = HttpResponse(open(filepath,'rb')) #Set http file response. Send recommenddb file to android
    response['Content-Disposition'] = 'attachment; filename="recommenddb"'
    return response


def GetUserFile(request, ID): #When android device terminated app, android_device send user_article_record_file to this function 
    if request.method == 'POST':
        form = UploadFileForm(request.POST, request.FILES) #error occur at valid procedure 
        if form.is_valid():
            print(request.FILES)
            handle_uploaded_file(request.FILES['file'],ID) #to download file, hand over task to handler.
            Run_User_Update.delay(ID) #Hand over the User_Update task to the celery.
            print("task went to celery...")
            return HttpResponse("success")

    else:
        form= UploadFileForm()
    return render(request, 'upload.html', {'form': form})


def handle_uploaded_file(file, ID): #this is filedownload handler
    dirpath = './recommender/userprofile/'+ID
    if not (os.path.isdir(dirpath)): #if directory not exist, make one
        os.mkdir(dirpath)

    with open('./recommender/userprofile/'+ID+'/recorddb', 'wb+') as destination: #file download
        destination.write(file.read())