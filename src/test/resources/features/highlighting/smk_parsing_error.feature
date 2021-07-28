Feature: Checks syntax errors, which were detected during parsing

  Scenario: Check 'use' section errors highlighting
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    module MODULE:
        snakefile: "file.smk"

    use a1 as NAME

    use rule as NAME2

    use rule NAME frm MODULE as NAME3

    use rule NAME from # Module name

    use rule * from MODULE with:
        input: "datafile.doc"

    use rule NAME as # No identifier

    use rule NAME as d**
    """
    Then I expect inspection error on < > in < a1> with message
    """
    'rule' keyword is expected after 'use'
    """
    Then I expect inspection error on < > in < as NAME2> with message
    """
    '*' or rule name expected
    """
    Then I expect inspection error on < > in < frm> with message
    """
    'from' expected
    """
    Then I expect inspection error on < > in <from # Module name> with message
    """
    Identifier expected
    """
    Then I expect inspection error on < > in <MODULE with:> with message
    """
    Keyword 'with' is not allowed with rule pattern '*'
    """
    Then I expect inspection error on < > in <as # No identifier> with message
    """
    Identifier expected
    """
    Then I expect inspection error on <*> in <d**> with message
    """
    Can't be '*' after '*' in the new rule name
    """
    When I check highlighting errors

  Scenario: Check 'use' section errors highlighting part 2. Missed 'with' keyword
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule NAME as NAME4:
        input: "data_file3.doc"
    """
    Then I expect inspection error with message "'with' expected" on
    """
    :
    """
    When I check highlighting errors