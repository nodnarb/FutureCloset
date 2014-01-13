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
#import "Transmitter.h"

@implementation Transmitter

- (id)init {
    self = [super init];
    self.dirty = [NSNumber numberWithBool:NO];
    votes = [[NSMutableArray alloc] initWithCapacity:100];
    return self;
}

- (void)addRSSI:(NSNumber *)rssi {
    if ([votes count] == 25) {
        [votes removeObjectAtIndex:0];
    }
    [votes addObject:rssi];
}

- (NSNumber *)getRssi {
    int sum = 0;
    for (NSNumber *number in votes) {
        sum += [number integerValue];
    }

    return [NSNumber numberWithFloat:((float)sum / (float)[votes count])];
}

@end
