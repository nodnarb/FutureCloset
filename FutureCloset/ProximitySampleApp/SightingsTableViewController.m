/**
 * Copyright (C) 2013 Qualcomm Retail Solutions, Inc. All rights reserved.
 *
 * This software is the confidential and proprietary information of Qualcomm
 * Retail Solutions, Inc.
 *
 * The following sample code illustrates various aspects of the FYX iOS SDK.
 *
 * The sample code herein is provided for your convenience, and has not been
 * tested or designed to work on any particular system configuration. It is
 * provided pursuant to the License Agreement for FYX Software and Developer
 * Portal AS IS, and your use of this sample code, whether as provided or with
 * any modification, is at your own risk. Neither Qualcomm Retail Solutions,
 * Inc. nor any affiliate takes any liability nor responsibility with respect
 * to the sample code, and disclaims all warranties, express and implied,
 * including without limitation warranties on merchantability, fitness for a
 * specified purpose, and against infringement.
 */
#import "SightingsTableViewController.h"
#import "SightingsTableViewCell.h"
#import "ApplicationContext.h"
#import "Transmitter.h"
#import <FYX/FYXTransmitter.h>
#import <FYX/FYXVisit.h>
#import <FYX/FYXTransmitterManager.h>
#import <QuartzCore/QuartzCore.h>
#import <FYX/FYXVisitManager.h>
#import <FYX/FYX.h>                      
#import "EnableProximityViewController.h"
#import "Clothing.h"
#import "ViewController.h"
#import "ClothingDetailViewController.h"
#import "LaundryStatusViewController.h"
#import "M2x.h"
#import "FeedsClient.h"

@interface SightingsTableViewController () {
    FeedsClient *feedClient;
}

@property (strong, nonatomic) NSMutableArray *transmitters;
@property (nonatomic) FYXVisitManager *visitManager;
@property (strong, nonatomic) UIView *noRegisteredTransmittersView;
@property (nonatomic) NSMutableArray *clothesArray;

@end

@implementation SightingsTableViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    
    //get singleton instance of M2x Class
    M2x* m2x = [M2x shared];
    //set the Master Api Key
    m2x.api_key = @"1a08ccc1f387096e8774946cc88a24e9";
    
    feedClient = [[FeedsClient alloc] init];
    [feedClient setFeed_key:@"1a08ccc1f387096e8774946cc88a24e9"];

    
    // Create the animated spinner view
    self.spinnerImageView = [UIImageView new];
    self.spinnerImageView.animationImages = [NSArray arrayWithObjects:
                                             [UIImage imageNamed:@"spinner_01.png"],
                                             [UIImage imageNamed:@"spinner_02.png"],
                                             [UIImage imageNamed:@"spinner_03.png"],
                                             [UIImage imageNamed:@"spinner_04.png"],
                                             [UIImage imageNamed:@"spinner_05.png"],
                                             [UIImage imageNamed:@"spinner_06.png"],
                                             [UIImage imageNamed:@"spinner_07.png"],
                                             [UIImage imageNamed:@"spinner_08.png"],
                                             [UIImage imageNamed:@"spinner_09.png"],
                                             [UIImage imageNamed:@"spinner_10.png"],
                                             [UIImage imageNamed:@"spinner_11.png"],
                                             [UIImage imageNamed:@"spinner_12.png"], nil];
    self.spinnerImageView.animationDuration = 1;
    self.spinnerImageView.animationRepeatCount = 0;
    [self finalizeUITheming];
    [self initializeTransmitters];
    [self initializeVisitManager];
    [self checkDirtyStatus];
}

- (void)viewDidUnload {
    [self cleanupVisitManager];
    [self setRefreshBarButton:nil];
    [super viewDidUnload];
}

- (BOOL)prefersStatusBarHidden
{
    return YES;
}

-(IBAction)whatToWearPressed:(id)sender {
    
}

- (void)dealloc
{
    [self cleanupVisitManager];
}

- (void)initializeVisitManager {
    NSLog(@"#### initializeVisitManager");
    if (!self.visitManager) {        
        self.visitManager = [[FYXVisitManager alloc] init];
        self.visitManager.delegate = self;
    }
    NSMutableDictionary *options = [NSMutableDictionary new];
    [options setObject:[NSNumber numberWithInt:15] forKey:FYXVisitOptionDepartureIntervalInSecondsKey];
    [options setObject:[NSNumber numberWithInt:FYXSightingOptionSignalStrengthWindowNone] forKey:FYXSightingOptionSignalStrengthWindowKey];
    [self.visitManager startWithOptions:options];
}

- (void)cleanupVisitManager {
    if (self.visitManager) {
        [self.visitManager stop];
    }
}

#pragma mark - User interface manipulation

- (void)finalizeUITheming {
    // Set the nav bar's background image
    //[self.navigationController.navigationBar setBackgroundImage:[UIImage imageNamed:@"nav_bar_tile.png"] forBarMetrics:UIBarMetricsDefault];
    //[self.navigationController.navigationBar setTranslucent:NO];
    
    // Replace the nav bar's title text with a custom image view
    UIImageView *imageView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"FTZlogo.png"]];
    imageView.contentMode = UIViewContentModeScaleAspectFit;
    [self.navigationController.navigationBar.topItem setTitleView:imageView];
    imageView.frame = CGRectMake(0, 0, 233, 24);
    
    self.title = @"FutureThreadz";
    
    self.navigationController.toolbarHidden = false;
    
    // Set the nav bar button background images
    //[self.refreshBarButton setBackgroundImage:[UIImage imageNamed:@"btn_nav.png"]forState:UIControlStateNormal barMetrics:UIBarMetricsDefault];
    
    [self hideNoTransmittersView];
}


- (void)hideNoTransmittersView {
    // Simply set a background image for the table view
    UIImageView *backgroundImageView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"background.png"]];
    [self.tableView setBackgroundView:backgroundImageView];
    [self.spinnerImageView stopAnimating];

}

- (void)showNoTransmittersView {
    CGRect viewFrame = self.tableView.frame;
    
    UIView *view = [[UIView alloc] initWithFrame:viewFrame];
    [view setBackgroundColor:[UIColor colorWithPatternImage:[UIImage imageNamed:@"background.png"]]];
    
    UILabel *label = [UILabel new];
    label.backgroundColor = [UIColor clearColor];
    label.font = [UIFont systemFontOfSize:22.0f];
    label.text = @"Scanning...";
    [label sizeToFit];
    label.center = CGPointMake(viewFrame.size.width / 2, (viewFrame.size.height / 2) - 40);
    [view addSubview:label];
    self.spinnerImageView.frame = CGRectMake(viewFrame.size.width / 2 - 25, (viewFrame.size.height / 2) - 105, 50, 50);
    [self.spinnerImageView startAnimating];
    [view addSubview:self.spinnerImageView];
    
    [self.tableView setBackgroundView:view];


}


- (float)barWidthForRSSI:(NSNumber *)rssi {
    NSInteger barMaxValue = [[NSUserDefaults standardUserDefaults] integerForKey:@"rssi_bar_max_value"];
    NSInteger barMinValue = [[NSUserDefaults standardUserDefaults] integerForKey:@"rssi_bar_min_value"];
    
    float rssiValue = [rssi floatValue];
    float barWidth;
    if (rssiValue >= barMaxValue) {
        barWidth = 270.0f;
    } else if (rssiValue <= barMinValue) {
        barWidth = 5.0f;
    } else {
        NSInteger barRange = barMaxValue - barMinValue;
        float percentage = (barMaxValue - rssiValue) / (float)barRange;
        barWidth = (1.0f - percentage) * 270.0f;
    }
    return barWidth;
}

- (NSNumber *)rssiForBarWidth:(float)barWidth {
    NSInteger barMaxValue = [[NSUserDefaults standardUserDefaults] integerForKey:@"rssi_bar_max_value"];
    NSInteger barMinValue = [[NSUserDefaults standardUserDefaults] integerForKey:@"rssi_bar_min_value"];
    
    NSInteger barRange = barMaxValue - barMinValue;
    float percentage = - ((barWidth / 270.0f) - 1.0f);
    float rssiValue = - ((percentage * (float)barRange) - barMaxValue);
    
    return [NSNumber numberWithFloat:rssiValue];
}

- (void)grayOutSightingsCell:(SightingsTableViewCell *)sightingsCell {
    
    if (sightingsCell) {
        dispatch_async(dispatch_get_main_queue(), ^{
            sightingsCell.contentView.alpha = 0.3f;
            CGRect oldFrame = sightingsCell.rssiImageView.frame;
            sightingsCell.rssiImageView.frame = CGRectMake(oldFrame.origin.x, oldFrame.origin.y, 0, oldFrame.size.height);
            sightingsCell.isGrayedOut = YES;
            sightingsCell.lastWornLabel.text = @"DIRTY";
        });
    }
}

- (UIImage *)getBatteryImageForLevel: (NSNumber *)batteryLevel{
    switch([batteryLevel integerValue]){
        case 0:
        case 1:
            return [UIImage imageNamed:@"battery_low.png"];
        case 2:
            return [UIImage imageNamed:@"battery_high.png"];
        case 3:
            return [UIImage imageNamed:@"battery_full.png"];
    }
    return [UIImage imageNamed:@"battery_unknown.png"];
}

- (void)updateSightingsCell:(SightingsTableViewCell *)sightingsCell withTransmitter:(Transmitter *)transmitter {
    
    if (sightingsCell && transmitter) {
        dispatch_async(dispatch_get_main_queue(), ^{
            sightingsCell.contentView.alpha = 1.0f;
            
            float oldBarWidth = [self barWidthForRSSI:transmitter.previousRSSI];
            float newBarWidth = [self barWidthForRSSI:transmitter.rssi];
            CGRect tempFrame = sightingsCell.rssiImageView.frame;
            CGRect oldFrame = CGRectMake(tempFrame.origin.x, tempFrame.origin.y, oldBarWidth, tempFrame.size.height);
            CGRect newFrame = CGRectMake(tempFrame.origin.x, tempFrame.origin.y, newBarWidth, tempFrame.size.height);
            
            // Animate updating the RSSI indicator bar
            sightingsCell.rssiImageView.frame = oldFrame;
            [UIView animateWithDuration:1.0f animations:^{
                sightingsCell.rssiImageView.frame = newFrame;
            }];
            sightingsCell.isGrayedOut = NO;
            UIImage *batteryImage = [self getBatteryImageForLevel:transmitter.batteryLevel];
            [sightingsCell.batteryImageView setImage:batteryImage];
            sightingsCell.temperature.text = [NSString stringWithFormat:@"%@%@", transmitter.temperature,
                                              [NSString stringWithUTF8String:"\xC2\xB0 F" ]];
            sightingsCell.rssiLabel.text = [NSString stringWithFormat:@"%@", transmitter.rssi];
            
        });
    }
}
- (void)addTransmitter: (Transmitter *)transmitter{
    @synchronized(self.transmitters){
        [self.transmitters addObject:transmitter];
        
    }
}

-(void)addClothing:(Clothing*)cloth {
    @synchronized(self.clothesArray) {
        [self.clothesArray addObject:cloth];
        if([self.transmitters count] == 1){
            [self hideNoTransmittersView];
        }
    }
}

- (BOOL)shouldUpdateTransmitterCell:(FYXVisit *)visit withTransmitter:(Transmitter *)transmitter RSSI:(NSNumber *)rssi{
    if (![transmitter.rssi isEqual:rssi] || ![transmitter.batteryLevel isEqualToNumber:visit.transmitter.battery]
        || ![transmitter.temperature isEqualToNumber:visit.transmitter.temperature]){
        return YES;
    }
    else {
        return NO;
    }
}

#pragma mark - Transmitters manipulation

- (Transmitter *)transmitterForID:(NSString *)ID {
    for (Transmitter *transmitter in self.transmitters) {
        if ([transmitter.identifier isEqualToString:ID]) {
            return transmitter;
        }
    }
    return nil;
}

- (void)initializeTransmitters {        
    // Re-create the transmitters container array
    [self showNoTransmittersView];
    @synchronized(self.transmitters){
        if (self.transmitters == nil) {
            self.transmitters = [NSMutableArray new];
        }
        
    }
    @synchronized(self.clothesArray) {
        if(self.clothesArray == nil) {
            self.clothesArray = [Clothing getClothes];
        }
    }
    // Always reload the table (even if the transmitter list didn't change)
    [self.tableView reloadData];
}

- (void)clearTransmitters {
    [self showNoTransmittersView];
    @synchronized(self.transmitters){
        [self.transmitters removeAllObjects];
        [self.tableView reloadData];
    }
}

- (void)removeTransmitter: (Transmitter*)transmitter {
    NSInteger count = 0;
    @synchronized(self.transmitters){
        [self.transmitters removeObject:transmitter];
        count =[self.transmitters count];
    }
    if(count == 0){
       [self showNoTransmittersView];
    }
}

- (BOOL)isTransmitterAgedOut:(Transmitter *)transmitter {
    
    NSDate *now = [NSDate date];
    NSTimeInterval ageOutPeriod = [[NSUserDefaults standardUserDefaults] integerForKey:@"age_out_period"];
    
    if ([now timeIntervalSinceDate:transmitter.lastSighted] > ageOutPeriod) {
        return YES;
    }
    return NO;
}

- (void)updateTransmitter:(Transmitter *)transmitter withVisit:(FYXVisit *)visit RSSI:(NSNumber *)rssi {
    transmitter.previousRSSI = transmitter.rssi;
    transmitter.rssi = rssi;
    transmitter.batteryLevel = visit.transmitter.battery;
    transmitter.temperature = visit.transmitter.temperature;
}

#pragma mark - Table view data source

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [self.transmitters count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    
    static NSString *CellIdentifier = @"MyReusableCell";
    SightingsTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    
    if (cell != nil) {
        Transmitter *transmitter = [self.transmitters objectAtIndex:indexPath.row];
        Clothing *cloth = [self.clothesArray objectAtIndex:indexPath.row];
        
        // Update the transmitter text
        cell.transmitterNameLabel.text = cloth.name;
        cell.transmitterIcon.image = cloth.picture;
        cell.lastWornLabel.text = [NSString stringWithFormat:@"Last Worn: %@", cloth.lastWornDate];
        if(cloth.numberFriendsWearing > 0) {
            cell.friendsWearingLabel.textColor = [UIColor redColor];
        } else {
            cell.friendsWearingLabel.textColor = [UIColor lightGrayColor];
        }
        cell.friendsWearingLabel.text = [NSString stringWithFormat:@"%d friends wearing", cloth.numberFriendsWearing];
        
        // Update the transmitter avatar (icon image)
        NSInteger avatarID = [UserSettingsRepository getAvatarIDForTransmitterID:transmitter.identifier];
        NSString *imageFilename = [NSString stringWithFormat:@"avatar_%02d.png", avatarID];
        //cell.transmitterIcon.image = [UIImage imageNamed:imageFilename];
        
        if (cloth.dirty) {
            [self grayOutSightingsCell:cell];
        } else {
            [self updateSightingsCell:cell withTransmitter:transmitter];
        }
    }
    return cell;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {

    if (editingStyle == UITableViewCellEditingStyleDelete) {
        Transmitter *transmitter = [self.transmitters objectAtIndex:indexPath.row];
        [self removeTransmitter:transmitter];
        [self.tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:UITableViewRowAnimationAutomatic];
        
    }
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    Clothing *item = [self.clothesArray objectAtIndex:indexPath.row];
    ClothingDetailViewController *controller = [[ClothingDetailViewController alloc] initWithNibName:@"ClothingDetailViewController" bundle:nil];
    [controller setClothing:item];
    
    [self.navigationController pushViewController:controller animated:YES];
    
}

#pragma mark - FYX visit delegate

- (void)didArrive:(FYXVisit *)visit {
    NSLog(@"############## didArrive: %@", visit);
}

- (void)didDepart:(FYXVisit *)visit {
    NSLog(@"############## didDepart: %@", visit);
    Transmitter *transmitter = [self transmitterForID:visit.transmitter.identifier];
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:[self.transmitters indexOfObject:transmitter] inSection:0];
    UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:indexPath];
    if ([cell isKindOfClass:[SightingsTableViewCell class]]) {
        [self grayOutSightingsCell:((SightingsTableViewCell*)cell)];
    }
}

- (void)receivedSighting:(FYXVisit *)visit updateTime:(NSDate *)updateTime RSSI:(NSNumber *)RSSI {
    NSLog(@"############## receivedSighting: %@", visit);
    
    Transmitter *transmitter = [self transmitterForID:visit.transmitter.identifier];
    if (!transmitter) {
        NSString *transmitterName = visit.transmitter.identifier;
        if(visit.transmitter.name){
            transmitterName = visit.transmitter.name;
        }
        transmitter = [Transmitter new];
        transmitter.identifier = visit.transmitter.identifier;
        transmitter.name = transmitterName;
        transmitter.lastSighted = [NSDate dateWithTimeIntervalSince1970:0];
        transmitter.rssi = [NSNumber numberWithInt:-100];
        transmitter.previousRSSI = transmitter.rssi;
        transmitter.batteryLevel = 0;
        transmitter.temperature = 0;
        
        Clothing *clothes = [[Clothing alloc] init];
        [clothes setTransmitter:transmitter];
        
        [self addClothing:clothes];
        [self addTransmitter:transmitter];
        [self.tableView reloadData];
    }
    
    Clothing *clothes = nil;
    for(Clothing *c in self.clothesArray) {
        if(c.transmitter == transmitter) {
            clothes = c;
        }
    }
    
    transmitter.lastSighted = updateTime;
    if([self shouldUpdateTransmitterCell:visit withTransmitter:transmitter RSSI:RSSI]){
        [self updateTransmitter:transmitter withVisit:visit  RSSI:RSSI];
        
        NSIndexPath *indexPath = [NSIndexPath indexPathForRow:[self.transmitters indexOfObject:transmitter] inSection:0];
        for (UITableViewCell *cell in self.tableView.visibleCells) {
            if ([[self.tableView indexPathForCell:cell] isEqual:indexPath]) {
                SightingsTableViewCell *sightingsCell = (SightingsTableViewCell *)cell;
                
                CALayer *tempLayer = [sightingsCell.rssiImageView.layer presentationLayer];
                transmitter.previousRSSI =  [self rssiForBarWidth:[tempLayer frame].size.width];
                
                [self updateSightingsCell:sightingsCell withTransmitter:transmitter];
                if (clothes.dirty) {
                    [self grayOutSightingsCell:sightingsCell];
                } else {
                    [self updateSightingsCell:sightingsCell withTransmitter:transmitter];
                }
            }
        }
    }
 }


#pragma mark - Storyboard interation

- (IBAction)refreshButtonClicked:(id)sender {
    [self clearTransmitters];
}

-(IBAction)laundryStatusPressed:(id)sender {
    LaundryStatusViewController *controller = [[LaundryStatusViewController alloc] initWithNibName:@"LaundryStatusViewController" bundle:nil];
    [self.navigationController pushViewController:controller animated:YES];
}

-(void)checkDirtyStatus {
    
    [feedClient listDataStreamsForFeedId:@"aa339e4f6f75c439e40274b986071d80" success:^(id object) {
        NSLog(@"Got Data Streams: %@", object);
        
        NSArray *array = [object objectForKey:@"streams"];
        for(NSDictionary *dict in array) {
            NSString *name = [dict objectForKey:@"name"];
            Clothing *cloth = [self getClothingForLowercaseBeacon:name];
            bool dirty = [[dict objectForKey:@"value"] integerValue];
            cloth.dirty = dirty;
        }
        [self.tableView reloadData];
        
        [self performSelector:@selector(checkDirtyStatus) withObject:nil afterDelay:2.0];
    }
                                 failure:^(NSError *error, NSDictionary *message) {
        NSLog(@"Error: %@",[error localizedDescription]);
        NSLog(@"Message: %@",message);
                                     [self performSelector:@selector(checkDirtyStatus) withObject:nil afterDelay:2.0];
        
    } ];
    
}

-(Clothing*)getClothingForLowercaseBeacon:(NSString*)name {
    for(Clothing *cloth in self.clothesArray) {
        NSString *lowername = [cloth.transmitter.name lowercaseString];
        NSString *noSpace = [lowername stringByReplacingOccurrencesOfString:@" " withString:@""];
        if([noSpace isEqualToString:name]) {
            return cloth;
        }
    }
    return nil;
}


@end
