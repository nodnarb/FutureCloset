//
//  ViewController.h
//  Weather
//
//  Created by Robert Ryan on 11/6/12.
//  Copyright (c) 2012 Robert Ryan. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ViewController : UIViewController

@property (weak, nonatomic) IBOutlet UILabel *zipCodePromptLabel;
@property (weak, nonatomic) IBOutlet UITextField *zipCodeTextField;
@property (weak, nonatomic) IBOutlet UIButton *zipCodeGoButton;

@property (weak, nonatomic) IBOutlet UILabel *statusLabel;
@property (weak, nonatomic) IBOutlet UILabel *pressureMbLabel;
@property (weak, nonatomic) IBOutlet UILabel *tempCLabel;
@property (weak, nonatomic) IBOutlet UILabel *locationLabel;
@property (weak, nonatomic) IBOutlet UILabel *weatherTypeLabel;
@property (weak, nonatomic) IBOutlet UIImageView *weatherTypeImage;
@property (weak, nonatomic) IBOutlet UILabel *clothing1Name;
@property (weak, nonatomic) IBOutlet UILabel *clothing2Name;
@property (weak, nonatomic) IBOutlet UILabel *clothing3Name;
@property (weak, nonatomic) IBOutlet UIImageView *clothing1Image;
@property (weak, nonatomic) IBOutlet UIImageView *clothing2Image;
@property (weak, nonatomic) IBOutlet UIImageView *clothing3Image;


- (IBAction)pressedZipCodeGoButton:(id)sender;
-(void)setClothes:(NSMutableArray*)clothes;

@end
