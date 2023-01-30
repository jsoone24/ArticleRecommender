from django import forms
from django.core.exceptions import ValidationError

#sets Form of UploadedFile from android
class UploadFileForm(forms.Form):
    title = forms.CharField(max_length=1000)
    file = forms.FileField()

    def is_valid(self) -> bool:
        return True