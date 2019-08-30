Feature: Completion in wrapper section arguments.
  Please check testData/wrapper/storage_data.txt to see/modify what data is 'cached' for these tests

  Scenario Outline: complete tool without tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "sam"
    """
    And I prepare wrapper storage
    When I put the caret after sam
    And I invoke autocompletion popup
    Then completion list should contain:
      | 0.36.0/bio/samtools/sort            |
      | 0.36.0/bio/samtools/view            |
      | 0.36.0/bio/samtools/sort            |
      | 0.36.0/bio/samtools/bam2fq/separate |
      | 0.36.0/bio/samtools/faidx           |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: complete tool with tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.33.0/"
    """
    And I prepare wrapper storage
    When I put the caret after 0.33.0/
    And I invoke autocompletion popup
    Then completion list should contain:
      | bio/samtools/sort            |
      | bio/samtools/sort            |
      | bio/samtools/view            |
      | bio/samtools/sort            |
      | bio/samtools/bam2fq/separate |
      | bio/samtools/faidx           |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: complete by name in nested directory without tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "sor"
    """
    And I prepare wrapper storage
    When I put the caret after sor
    And I invoke autocompletion popup
    Then completion list should contain:
      | 0.36.0/bio/samtools/sort |
      | 0.36.0/bio/sambamba/sort |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: complete by name in nested directory with tag older than cached
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.33.0/vie"
    """
    And I prepare wrapper storage
    When I put the caret after vie
    And I invoke autocompletion popup
    Then completion list should contain:
      | bio/samtools/view |
      | bio/bcftools/view |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
