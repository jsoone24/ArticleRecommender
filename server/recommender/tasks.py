"""Celery task wiring for the recommender recompute.

Triggered by ``views.GetUserFile`` after a fresh ``recorddb`` upload.
Runs ``User_update`` (drift the user vector) followed by ``DB_similarity``
(rank every article against it and write the per-user ``recommenddb``).

The two stages are sequential because ``DB_similarity`` reads the
``UserData.csv`` that ``User_update`` writes.
"""

from __future__ import absolute_import, unicode_literals
import os

from celery import shared_task

from .py import User_update, DB_similarity


@shared_task
def Run_User_Update(ID):
    """Recompute one user's recommendations end-to-end."""
    print("inside celery", ID)
    dirpath = './recommender/userprofile/' + ID
    os.makedirs(dirpath, exist_ok=True)

    print("running User_Update...")
    User_update.main(ID)
    print("running DB_similarity...")
    DB_similarity.main(ID)
    print("finished")