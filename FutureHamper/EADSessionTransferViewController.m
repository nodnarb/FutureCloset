/*
 
     File: EADSessionTransferViewController.m
 Abstract: n/a
  Version: 1.1
 
 Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
 Inc. ("Apple") in consideration of your agreement to the following
 terms, and your use, installation, modification or redistribution of
 this Apple software constitutes acceptance of these terms.  If you do
 not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.
 
 In consideration of your agreement to abide by the following terms, and
 subject to these terms, Apple grants you a personal, non-exclusive
 license, under Apple's copyrights in this original Apple software (the
 "Apple Software"), to use, reproduce, modify and redistribute the Apple
 Software, with or without modifications, in source and/or binary forms;
 provided that if you redistribute the Apple Software in its entirety and
 without modifications, you must retain this notice and the following
 text and disclaimers in all such redistributions of the Apple Software.
 Neither the name, trademarks, service marks or logos of Apple Inc. may
 be used to endorse or promote products derived from the Apple Software
 without specific prior written permission from Apple.  Except as
 expressly stated in this notice, no other rights or licenses, express or
 implied, are granted by Apple herein, including but not limited to any
 patent rights that may be infringed by your derivative works or by other
 works in which the Apple Software may be incorporated.
 
 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
 MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
 OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
 
 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
 AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 
 Copyright (C) 2010 Apple Inc. All Rights Reserved.
 
 
 */

#import "EADSessionTransferViewController.h"
#import "EADSessionController.h"
#import <FYX/FYXTransmitter.h>
#import <FYX/FYXVisit.h>
#import <FYX/FYXTransmitterManager.h>
#import <FYX/FYX.h>
#import "Transmitter.h"
#import "M2x.h"
#import "FeedsClient.h"

@interface EADSessionTransferViewController()
@property (strong, nonatomic) NSMutableArray *transmitters;
@property (nonatomic) FYXVisitManager *visitManager;
@property (nonatomic, strong) FeedsClient *feedsClient;
@end

@implementation EADSessionTransferViewController

@synthesize
    receivedBytesLabel = _receivedBytesLabel,
    stringToSendTextField = _stringToSendTextField,
    hexToSendTextField = _hexToSendTextField,
    feedsClient = _feedsClient;


// send test string to the accessory
- (IBAction)sendString:(id)sender;
{
    if ([_stringToSendTextField isFirstResponder]) {
        [_stringToSendTextField resignFirstResponder];
    }

    const char *buf = [[_stringToSendTextField text] UTF8String];
    if (buf)
    {
        uint32_t len = strlen(buf) + 1;
        [[EADSessionController sharedController] writeData:[NSData dataWithBytes:buf length:len]];
    }
}

// Interpret a UITextField's string at a sequence of hex bytes and send those bytes to the accessory
- (IBAction)sendHex:(id)sender;
{
    if ([_hexToSendTextField isFirstResponder]) {
        [_hexToSendTextField resignFirstResponder];
    }

    const char *buf = [[_hexToSendTextField text] UTF8String];
    NSMutableData *data = [NSMutableData data];
    if (buf)
    {
        uint32_t len = strlen(buf);

        char singleNumberString[3] = {'\0', '\0', '\0'};
        uint32_t singleNumber = 0;
        for(uint32_t i = 0 ; i < len; i+=2)
        {
            if ( ((i+1) < len) && isxdigit(buf[i]) && (isxdigit(buf[i+1])) )
            {
                singleNumberString[0] = buf[i];
                singleNumberString[1] = buf[i + 1];
                sscanf(singleNumberString, "%x", &singleNumber);
                uint8_t tmp = (uint8_t)(singleNumber & 0x000000FF);
                [data appendBytes:(void *)(&tmp) length:1];
            }
            else
            {
                break;
            }
        }

        [[EADSessionController sharedController] writeData:data];
    }
}

// send 10K of data to the accessory.
- (IBAction)send10K:(id)sender
{
#define STRESS_TEST_BYTE_COUNT 10000
    uint8_t buf[STRESS_TEST_BYTE_COUNT];
    for(int i = 0; i < STRESS_TEST_BYTE_COUNT; i++) {
        buf[i] = (i & 0xFF);  // fill buf with incrementing bytes;
    }

	[[EADSessionController sharedController] writeData:[NSData dataWithBytes:buf length:STRESS_TEST_BYTE_COUNT]];
}

#pragma mark UIViewController

- (void)viewWillAppear:(BOOL)animated
{
    // watch for the accessory being disconnected
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_accessoryDidDisconnect:) name:EAAccessoryDidDisconnectNotification object:nil];
    // watch for received data from the accessory
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_sessionDataReceived:) name:EADSessionDataReceivedNotification object:nil];

    EADSessionController *sessionController = [EADSessionController sharedController];

    _accessory = [sessionController accessory];
    [self setTitle:[sessionController protocolString]];
    [sessionController openSession];
}

- (void)viewWillDisappear:(BOOL)animated
{
    // remove the observers
    [[NSNotificationCenter defaultCenter] removeObserver:self name:EAAccessoryDidConnectNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:EADSessionDataReceivedNotification object:nil];

    EADSessionController *sessionController = [EADSessionController sharedController];

    [sessionController closeSession];
    _accessory = nil;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    //get singleton instance of M2x Class
    M2x* m2x = [M2x shared];
    //set the Master Api Key
    m2x.api_key = @"1a08ccc1f387096e8774946cc88a24e9";
    
    self.feedsClient = [[FeedsClient alloc] init];
    [_feedsClient setFeed_key:@"1a08ccc1f387096e8774946cc88a24e9"];

    [self initializeTransmitters];
    [self initializeVisitManager];
    [self postState];
}

- (void)viewDidUnload
{
    [self cleanupVisitManager];
    [super viewDidUnload];
    self.feedsClient = nil;
    self.receivedBytesLabel = nil;
    self.stringToSendTextField = nil;
    self.hexToSendTextField = nil;
}

#pragma mark UITextFieldDelegate

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    [textField resignFirstResponder];
    return YES;
}

#pragma mark Internal

- (void)_accessoryDidDisconnect:(NSNotification *)notification
{
    if ([[self navigationController] topViewController] == self)
    {
        EAAccessory *disconnectedAccessory = [[notification userInfo] objectForKey:EAAccessoryKey];
        if ([disconnectedAccessory connectionID] == [_accessory connectionID])
        {
            [[self navigationController] popViewControllerAnimated:YES];

        }
    }
}

// Data was received from the accessory, real apps should do something with this data but currently:
//    1. bytes counter is incremented
//    2. bytes are read from the session controller and thrown away
- (void)_sessionDataReceived:(NSNotification *)notification
{
    EADSessionController *sessionController = (EADSessionController *)[notification object];
    uint32_t bytesAvailable = 0;

    while ((bytesAvailable = [sessionController readBytesAvailable]) > 0) {
        NSData *data = [sessionController readData:bytesAvailable];
        if (data) {
            _totalBytesRead += bytesAvailable;
        }
    }

    [_receivedBytesLabel setText:[NSString stringWithFormat:@"Bytes Received from Session: %d", _totalBytesRead]];
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
    @synchronized(self.transmitters){
        if (self.transmitters == nil) {
            self.transmitters = [NSMutableArray new];

            Transmitter *transmitter1 = [[Transmitter alloc] init];
            transmitter1.name = @"Beacon 1";
            transmitter1.identifier = @"XUX6-T6AYT";
            [self.transmitters addObject:transmitter1];
            Transmitter *transmitter2 = [[Transmitter alloc] init];
            transmitter2.name = @"Beacon 2";
            transmitter2.identifier = @"1A42-Y64X2";
            [self.transmitters addObject:transmitter2];
            Transmitter *transmitter3 = [[Transmitter alloc] init];
            transmitter3.name = @"Beacon 3";
            transmitter3.identifier = @"5VQ2-FWPVN";
            [self.transmitters addObject:transmitter3];
            Transmitter *transmitter4 = [[Transmitter alloc] init];
            transmitter4.name = @"Beacon 4";
            transmitter4.identifier = @"ESCG-TYUUQ";
            [self.transmitters addObject:transmitter4];
            Transmitter *transmitter5 = [[Transmitter alloc] init];
            transmitter5.name = @"Beacon 5";
            transmitter5.identifier = @"ZJ8B-1H18V";
            [self.transmitters addObject:transmitter5];
            Transmitter *transmitter6 = [[Transmitter alloc] init];
            transmitter6.name = @"Beacon 6";
            transmitter6.identifier = @"9JJu-2J4MV";
            [self.transmitters addObject:transmitter6];
            Transmitter *transmitter7 = [[Transmitter alloc] init];
            transmitter7.name = @"Beacon 7";
            transmitter7.identifier = @"391U-HXT3V";
            [self.transmitters addObject:transmitter7];
            Transmitter *transmitter8 = [[Transmitter alloc] init];
            transmitter8.name = @"Beacon 8";
            transmitter8.identifier = @"M1F2-J4N4P";
            [self.transmitters addObject:transmitter8];
            Transmitter *transmitter9 = [[Transmitter alloc] init];
            transmitter9.name = @"Beacon 9";
            transmitter9.identifier = @"J7WC-8GQY3";
            [self.transmitters addObject:transmitter9];
            Transmitter *transmitter10 = [[Transmitter alloc] init];
            transmitter10.name = @"Beacon 10";
            transmitter10.identifier = @"BQ6J-XJZKH";
            [self.transmitters addObject:transmitter10];
            Transmitter *transmitter11 = [[Transmitter alloc] init];
            transmitter11.name = @"Beacon 11";
            transmitter11.identifier = @"TGAT-6RRT1";
            [self.transmitters addObject:transmitter11];
            Transmitter *transmitter12 = [[Transmitter alloc] init];
            transmitter12.name = @"Beacon 12";
            transmitter12.identifier = @"4BYY-37RB1";
            [self.transmitters addObject:transmitter12];
            Transmitter *transmitter13 = [[Transmitter alloc] init];
            transmitter13.name = @"Beacon 13";
            transmitter13.identifier = @"83FP-AKW7Y";
            [self.transmitters addObject:transmitter13];
            Transmitter *transmitter14 = [[Transmitter alloc] init];
            transmitter14.name = @"Beacon 14";
            transmitter14.identifier = @"YXP8-R5URM";
            [self.transmitters addObject:transmitter14];
        }
        // Always reload the table (even if the transmitter list didn't change)
    }
}

- (void)clearTransmitters {
    @synchronized(self.transmitters){
        [self.transmitters removeAllObjects];
    }
}

- (void)addTransmitter: (Transmitter *)transmitter{
    @synchronized(self.transmitters){
        [self.transmitters addObject:transmitter];
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
    transmitter.previousRSSI = [transmitter getRssi];
    [transmitter addRSSI:rssi];
    transmitter.batteryLevel = visit.transmitter.battery;
    transmitter.temperature = visit.transmitter.temperature;
}

#pragma mark - FYX visit delegate

- (void)didArrive:(FYXVisit *)visit {
    NSLog(@"############## didArrive: %@", visit);
}

- (void)didDepart:(FYXVisit *)visit {
    NSLog(@"############## didDepart: %@", visit);
}

- (void)receivedSighting:(FYXVisit *)visit updateTime:(NSDate *)updateTime RSSI:(NSNumber *)RSSI {
//    NSLog(@"############## receivedSighting: %@", visit);
    @synchronized(self.transmitters) {
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
        [transmitter addRSSI:[NSNumber numberWithInt:-100]];
        transmitter.previousRSSI = [transmitter getRssi];
        transmitter.batteryLevel = 0;
        transmitter.temperature = 0;
        [self addTransmitter:transmitter];
    } else {
        transmitter.previousRSSI = [transmitter getRssi];
        [transmitter addRSSI:RSSI];
//        NSLog(@"RSSI: %@", RSSI);
        NSLog(@"name: %@ rssi: %@", transmitter.name, [transmitter getRssi]);

        if ([transmitter getRssi] > [NSNumber numberWithInt:-40] && [transmitter.dirty isEqualToNumber:[NSNumber numberWithBool:NO]]) {
            transmitter.dirty = [NSNumber numberWithBool:YES];
        } else if ([transmitter getRssi] < [NSNumber numberWithInt:-70] && [transmitter.dirty isEqualToNumber:[NSNumber numberWithBool:YES]]){
            transmitter.dirty = [NSNumber numberWithBool:NO];
        }
    }

    transmitter.lastSighted = updateTime;
    }
}

- (void)postState {
    [self performSelectorOnMainThread:@selector(postStateOnThread) withObject:nil waitUntilDone:NO];
}

- (void)postStateOnThread {
    @synchronized(self.transmitters){
        if (!self.transmitters || [self.transmitters count] <= 0) {
            [self performSelector:@selector(postState) withObject:nil afterDelay:2];
            return;
        }
        for (Transmitter *transmitter in self.transmitters) {
            NSDictionary *innerDict = [NSDictionary dictionaryWithObject:[transmitter.dirty copy] forKey:@"value"];
            NSString *streamName = [transmitter.name copy];
            NSString *streamName2 = [streamName lowercaseString];
            NSString *streamName3 = [streamName2 stringByReplacingOccurrencesOfString:@" " withString:@""];
            NSArray *array = [NSArray arrayWithObjects:innerDict, nil];
            NSDictionary *dict = [NSDictionary dictionaryWithObject:array forKey:@"values"];
            NSLog(@"posting %@ dirty: %@", transmitter.name, transmitter.dirty);
            [_feedsClient postDataValues:dict
                               forStream:streamName3
                                  inFeed:@"aa339e4f6f75c439e40274b986071d80"
                                 success:^(id object) { /*NSLog(@"post state success");*/ }
                                 failure:^(NSError *error, NSDictionary *message)
             {
                 //NSLog(@"Error: %@",[error localizedDescription]);
                 //NSLog(@"Message: %@",message);
             }];
        }
    }

    //    NSDictionary *newValue = @{ @"values": @[ @{ @"value": @"Beacon 1" }, @{ @ ] };
    [self performSelector:@selector(postState) withObject:nil afterDelay:2];
}

@end
