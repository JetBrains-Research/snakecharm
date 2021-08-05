use rule from module as f with:
    input: "text"

use rule rule from as f with:
    input: "text"

use a1 as NAME

use rule as NAME2

use rule NAME frm MODULE as NAME3

use rule * from MODULE with:
    input: "datafile.doc"

use rule NAME as d**

use rule NAME,*,NAME2 from MODULE as other_*

use rule * as other_*

use rule NAME,NAME2 from MODULE as N1,N2

use rule NAME, NAME2 from MODULE as N1 with:
    input: "file"

use rule a,,b,c from MODULE as other_*

use rule z from M
    input: "myfile3"

use rule z from M input: "myfile3"
