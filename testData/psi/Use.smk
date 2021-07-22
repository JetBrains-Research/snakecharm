use rule a from my_module as b

use rule f from other_module as f_z with:
    input: "data.csv"

use rule f_z as x_z with:
    output: "dir/file.svg"
use rule * from last_module1 as *_other
use rule * from last_module2 as other_*
use rule * from last_module3 as other_*_other