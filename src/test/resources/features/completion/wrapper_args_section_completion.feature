Feature: Completion for arguments used in wrapper

  Scenario Outline: Simple completion for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list should contain:
      | <completion> |
    Examples:
      | rule_like  | section | wrapper                    | completion       |
      | rule       | input   | bio/bcftools/reheader      | vcf              |
      | rule       | input   | bio/bismark/bismark2report | splitting_report |
      | rule       | input   | bio/bismark/bismark        | fq_1             |
      | rule       | params  | bio/gatk/applybqsr         | java_opts        |
      | checkpoint | params  | bio/bcftools/call          | mpileup          |
      | checkpoint | params  | utils/cairosvg             | extra            |
      | checkpoint | output  | bio/last/lastal            | blasttab         |

  Scenario Outline: Do not suggest service sections as sections in completion
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <completion> |
    Examples:
      | rule_like | section | wrapper     | completion  |
      | rule      | params  | bio/bwa/mem | name        |
      | rule      | params  | bio/bwa/mem | description |
      | rule      | params  | bio/bwa/mem | url         |
      | rule      | params  | bio/bwa/mem | authors     |
      | rule      | params  | bio/bwa/mem | notes       |

  Scenario Outline: Simple completion for bundled wrappers using different declaration styles
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:
      wrapper: <wrapper>
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list should contain:
      | <completion> |
    Examples:
      | rule_like | section | wrapper                         | completion |
      | rule      | input   | "0.64.0/bio/bcftools/reheader"  | vcf        |
      | rule      | input   | '0.64.0/bio/bcftools/reheader'  | vcf        |
      | rule      | input   | f"0.64.0/bio/bcftools/reheader" | vcf        |
      | rule      | input   | f'0.64.0/bio/bcftools/reheader' | vcf        |
      | rule      | input   | f'{tag}/bio/bcftools/reheader'  | vcf        |

  Scenario Outline: No completion for sections from wrong bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <section>:
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <completion> |
    Examples:
      | rule_like | section | wrapper                    | completion       |
      | rule      | input   | bio/bismark/bismark2report | bam              |
      | rule      | input   | bio/bismark/bismark        | splitting_report |

  Scenario Outline: No completion for bundled wrappers in wrong context
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>:  <text>
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret at <signature>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <completion> |
    Examples:
      | rule_like | section | text      | signature | wrapper               | completion |
      | rule      | input   | "#here"   | #here     | bio/bcftools/reheader | vcf        |
      | rule      | params  | a=#here   | #here     | utils/cairosvg        | extra      |
      | rule      | params  | a=here    | here      | utils/cairosvg        | extra      |
      | rule      | params  | foo(here) | here      | utils/cairosvg        | extra      |
