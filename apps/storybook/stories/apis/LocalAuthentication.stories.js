import { storiesOf } from '@storybook/react-native';
import React from 'react';

import UIExplorer, { AppText, Description } from '../ui-explorer';
import notes from './LocalAuthentication.notes.md';

const TITLE = 'LocalAuthentication';

class DemoScreen extends React.Component {
  render() {
    return (
      <UIExplorer title={TITLE}>
        <Description>
          <AppText>Desc</AppText>
        </Description>
      </UIExplorer>
    );
  }
}

// storiesOf('APIs', module).add(TITLE, () => <DemoScreen />, { notes });
