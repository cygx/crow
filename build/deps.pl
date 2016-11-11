use strict;
use warnings;
use File::Find;

my %classes;
my @classes;

sub gather {
    return unless -f && s/\.java$//;
    die "$_ already not unique\n"
        if exists $classes{$_};

    my $class = $File::Find::name;
    $class =~ s/^sources/classes/;
    $class =~ s/java$/class/;

    $classes{$_} = {
        name => $_,
        src => $File::Find::name,
        class => $class,
        deps => {},
    };
}

find \&gather, 'sources';
@classes = @classes{sort keys %classes};

for my $c (@classes) {
    open my $fh, '<', $c->{src};
    my %words;
    @words{/\b\w+\b/g} = () while <$fh>;
    close $fh;

    for my $w (keys %words) {
        $c->{deps}->{$w} = undef
            if exists $classes{$w};
    }

    delete $c->{deps}->{$c->{name}};
    $c->{deps} = [ sort keys %{$c->{deps}} ];
}

print ".classes.dummy:", map({ ' '.$_->{src} } @classes),
    "\n\tjavac -d classes -sourcepath sources \$^",
    "\n\t\@touch \$@\n\n";

print $_->{name}, ': ', $_->{class}, "\n"
    for @classes;

print "\n", $_->{class}, ": ", $_->{src},
    map({ ' '.$classes{$_}->{src} } @{$_->{deps}}),
    "\n\tjavac -d classes -sourcepath sources \$<\n"
    for @classes;
