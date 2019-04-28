import React from 'react';

import UIExplorer, { AppText, Description, Section } from '../ui-explorer';
import { storiesOf } from '@storybook/react-native';

const TITLE = 'AdMob';

class DemoScreen extends React.Component {
  render() {
    return (
      <UIExplorer title={TITLE} url="components/AdMob">
        <Description>Desc</Description>
      </UIExplorer>
    );
  }
}

import notes from './AdMob.notes.md';

// storiesOf('Components', module).add(TITLE, () => <DemoScreen />, { notes });
