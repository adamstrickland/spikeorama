from ..models import OUTBOX_TABLE
from .basic_outbox import BasicDjangoOutbox

class DjangoOutbox(BasicDjangoOutbox):
    class Meta:
        db_table = OUTBOX_TABLE
