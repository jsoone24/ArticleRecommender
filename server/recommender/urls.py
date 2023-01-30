from django.urls import path
from django.conf import settings
from django.conf.urls.static import static
from . import views

#if given url patterns equals to below, runs functions in views.py
urlpatterns = [
    path('SendUserFile/<str:ID>', views.SendUserFile, name='SendUserFile'),
    path('GetUserFile/<str:ID>', views.GetUserFile, name='GetUserFile')
]
urlpatterns += static(settings.MEDIA_URL, document_root = settings.MEDIA_ROOT)