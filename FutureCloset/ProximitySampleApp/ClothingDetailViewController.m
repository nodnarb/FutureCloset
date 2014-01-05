//
//  ClothingDetailViewController.m
//  ProximitySampleApp-withFrameworks
//
//  Created by Jon Carroll on 1/5/14.
//  Copyright (c) 2014 Qualcomm Retail Solutions, Inc. All rights reserved.
//

#import "ClothingDetailViewController.h"

@interface ClothingDetailViewController () {
    Clothing *clothes;
    IBOutlet UILabel *nameLabel;
    IBOutlet UIImageView *imageView;
    IBOutlet UILabel *typeLabel;
    IBOutlet UILabel *warmthLabel;
    IBOutlet UILabel *statusLabel;
    IBOutlet UIProgressView *proximityMeter;
    bool updatingProximity;
}

@end

@implementation ClothingDetailViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
        updatingProximity = false;
    }
    return self;
}

-(void)setClothing:(Clothing*)item {
    clothes = item;
    [self populateFields];
}

-(void)populateFields {
    self.title = clothes.name;
    nameLabel.text = [NSString stringWithFormat:@"Name: %@", clothes.name];
    typeLabel.text = [NSString stringWithFormat:@"Type: %@", [clothes getTypeAsString]];
    warmthLabel.text = [NSString stringWithFormat:@"Warmth: %d", clothes.warmth];
    NSString *status = @"Clean";
    if(clothes.dirty) {
        status = @"Dirty";
    }
    statusLabel.text = [NSString stringWithFormat:@"Status: %@",status];
    if(!updatingProximity) {
        [self updateProximity];
    }
    
    imageView.image = clothes.picture;
}

-(void)updateProximity {
    updatingProximity = YES;
    
    NSInteger barMaxValue = [[NSUserDefaults standardUserDefaults] integerForKey:@"rssi_bar_max_value"];
    NSInteger barMinValue = [[NSUserDefaults standardUserDefaults] integerForKey:@"rssi_bar_min_value"];
    
    float rssiValue = [clothes.transmitter.rssi floatValue];
    float barWidth;
    if (rssiValue >= barMaxValue) {
        barWidth = 1.0;
    } else if (rssiValue <= barMinValue) {
        barWidth = 0.05;
    } else {
        NSInteger barRange = barMaxValue - barMinValue;
        float percentage = (barMaxValue - rssiValue) / (float)barRange;
        barWidth = (1.0f - percentage);
    }
    [UIView animateWithDuration:0.2
                     animations:^{
                         proximityMeter.progress = barWidth;
                     }
                     completion:^(BOOL finished){
                         
                         [self updateProximity];
                         
                     }];
    
    
    
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
    [self populateFields];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (BOOL)prefersStatusBarHidden
{
    return YES;
}

@end
