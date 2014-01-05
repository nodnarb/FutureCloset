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
}

@end

@implementation ClothingDetailViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

-(void)setClothing:(Clothing*)item {
    clothes = item;
}

-(void)populateFields {
    self.title = clothes.name;
    nameLabel.text = clothes.name;

}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
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
