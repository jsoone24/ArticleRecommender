from django import forms


class UploadFileForm(forms.Form):
    """Validates the multipart upload from the Android client."""

    title = forms.CharField(max_length=1000, required=False)
    file = forms.FileField()
